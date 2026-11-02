import express from 'express';

const app = express();
app.set('trust proxy', 1);
app.use(express.json({ limit: '32kb' }));

const PORT = process.env.PORT || 3000;
const APIFY_TOKEN = process.env.APIFY_TOKEN;

const DETAIL_ACTOR = 'spider_studio~reelshort-book-detail';
const DOWNLOAD_ACTOR = 'spider_studio~reelshort-m3u8-downloader';

app.get('/health', (_req, res) =>
  res.json({ ok: true, service: 'ShortSave API' })
);

async function runActor(actor, input) {
  const endpoint =
    `https://api.apify.com/v2/acts/${actor}/run-sync-get-dataset-items` +
    `?token=${encodeURIComponent(APIFY_TOKEN)}&timeout=300&format=json`;

  const response = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input)
  });

  const raw = await response.text();

  if (!response.ok) {
    throw new Error(`Apify returned ${response.status}: ${raw.slice(0, 300)}`);
  }

  return JSON.parse(raw);
}

function extractBookId(value) {
  const url = new URL(value);

  if (!['reelshort.com', 'www.reelshort.com', 'app.reelshort.com'].includes(url.hostname)) {
    throw new Error('Please enter a valid ReelShort link.');
  }

  const match = url.pathname.match(/\/(?:app-video-share|video-share)\/([A-Za-z0-9_-]+)/);

  if (!match) {
    throw new Error('Could not find a ReelShort book ID in this link.');
  }

  return match[1];
}

// Paste a ReelShort link and retrieve its episode list.
app.post('/api/episodes', async (req, res) => {
  try {
    if (!APIFY_TOKEN) {
      return res.status(500).json({ success: false, error: 'Server is missing APIFY_TOKEN.' });
    }

    const { url } = req.body ?? {};

    if (typeof url !== 'string' || !url.trim()) {
      return res.status(400).json({ success: false, error: 'ReelShort URL is required.' });
    }

    const bookId = extractBookId(url.trim());
    const rows = await runActor(DETAIL_ACTOR, { bookIds: [bookId] });
    const book = Array.isArray(rows) ? rows.find(x => x.success) : null;

    if (!book) {
      return res.status(422).json({
        success: false,
        error: rows?.[0]?.error || 'Could not retrieve this story.'
      });
    }

    const episodes = (book.chapters || []).map(chapter => ({
      chapterId: chapter.chapterId,
      episode: chapter.serialNumber,
      title: chapter.chapterName || `Episode ${chapter.serialNumber}`,
      duration: chapter.duration || null,
      locked: chapter.isLocked === true
    })).filter(chapter => chapter.chapterId);

    res.json({
      success: true,
      bookId: book.bookId || bookId,
      title: book.title || 'ReelShort story',
      coverUrl: book.coverUrl || null,
      episodes
    });
  } catch (error) {
    res.status(400).json({ success: false, error: error.message });
  }
});

// Download one selected, accessible episode.
app.post('/api/download', async (req, res) => {
  try {
    if (!APIFY_TOKEN) {
      return res.status(500).json({ success: false, error: 'Server is missing APIFY_TOKEN.' });
    }

    const { bookId, chapterId } = req.body ?? {};

    if (
      typeof bookId !== 'string' || typeof chapterId !== 'string' ||
      !bookId.trim() || !chapterId.trim()
    ) {
      return res.status(400).json({
        success: false,
        error: 'bookId and chapterId are required.'
      });
    }

    const rows = await runActor(DOWNLOAD_ACTOR, {
      chapters: [{
        bookId: bookId.trim(),
        chapterId: chapterId.trim()
      }]
    });

    const item = Array.isArray(rows) ? rows.find(x => x.success === true) : null;

    if (!item || !item.kvsRecordUrl) {
      return res.status(422).json({
        success: false,
        error: rows?.[0]?.error || 'No accessible MP4 was returned.'
      });
    }

    const recordUrl = new URL(item.kvsRecordUrl);

    if (
      recordUrl.hostname !== 'api.apify.com' ||
      !recordUrl.pathname.includes('/v2/key-value-stores/')
    ) {
      return res.status(502).json({
        success: false,
        error: 'Actor returned an unexpected file URL.'
      });
    }

    const parts = recordUrl.pathname.split('/');
    const index = parts.indexOf('key-value-stores');
    const storeId = parts[index + 1];
    const key = parts[index + 3];

    if (!storeId || !key) {
      return res.status(502).json({ success: false, error: 'Invalid file reference.' });
    }

    res.json({
      success: true,
      fileName: item.fileName || `ShortSave-${chapterId}.mp4`,
      selectedWidth: item.selectedWidth || null,
      selectedHeight: item.selectedHeight || null,
      downloadUrl:
        `${req.protocol}://${req.get('host')}/api/file/` +
        `${encodeURIComponent(storeId)}/${encodeURIComponent(key)}`
    });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// Stream the MP4 from Apify without exposing the API token.
app.get('/api/file/:storeId/:key', async (req, res) => {
  try {
    if (!APIFY_TOKEN) return res.status(500).send('Server is missing APIFY_TOKEN.');

    const { storeId, key } = req.params;

    if (
      !/^[A-Za-z0-9_-]+$/.test(storeId) ||
      !/^[A-Za-z0-9_.-]+$/.test(key)
    ) {
      return res.status(400).send('Invalid file reference.');
    }

    const url =
      `https://api.apify.com/v2/key-value-stores/${encodeURIComponent(storeId)}` +
      `/records/${encodeURIComponent(key)}?token=${encodeURIComponent(APIFY_TOKEN)}`;

    const upstream = await fetch(url);

    if (!upstream.ok) {
      return res.status(upstream.status).send('Unable to retrieve MP4 from Apify.');
    }

    res.setHeader('Content-Type', 'video/mp4');
    res.setHeader(
      'Content-Disposition',
      `attachment; filename="${key.replace(/["\\\\]/g, '_')}"`
    );

    if (upstream.body) {
      const { Readable } = await import('node:stream');
      Readable.fromWeb(upstream.body).pipe(res);
    } else {
      res.status(502).end();
    }
  } catch {
    res.status(500).send('File proxy error.');
  }
});

app.listen(PORT, () => console.log(`ShortSave API listening on ${PORT}`));
