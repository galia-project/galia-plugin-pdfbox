/*
 * Copyright © 2024 Baird Creek Software LLC
 *
 * Licensed under the PolyForm Noncommercial License, version 1.0.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://polyformproject.org/licenses/noncommercial/1.0.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package is.galia.plugin.pdfbox;

import is.galia.codec.AbstractDecoder;
import is.galia.codec.Decoder;
import is.galia.codec.DecoderHint;
import is.galia.codec.SourceFormatException;
import is.galia.config.Configuration;
import is.galia.image.MutableMetadata;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.MediaType;
import is.galia.image.Metadata;
import is.galia.image.Region;
import is.galia.image.ReductionFactor;
import is.galia.plugin.Plugin;
import is.galia.stream.PathImageInputStream;
import is.galia.util.ArrayUtils;
import is.galia.util.IOUtils;
import is.galia.util.Stopwatch;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.io.RandomAccessReadMemoryMappedFile;
import org.apache.pdfbox.pdmodel.DefaultResourceCache;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public final class PDFBoxDecoder extends AbstractDecoder
        implements Decoder, Plugin {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PDFBoxDecoder.class);

    static final String DPI_CONFIG_KEY = "decoder.PDFBoxDecoder.dpi";

    /**
     * {@link #DPI_CONFIG_KEY} takes precedence over this, if set.
     */
    static final int DEFAULT_DPI = 150;

    private static final MediaType MEDIA_TYPE =
            new MediaType("application", "pdf");

    private static final byte[] PDF_SIGNATURE = new byte[] {
            0x25, 0x50, 0x44, 0x46, 0x2d };

    private static final Format FORMAT = new Format(
            "pdf",               // key
            "PDF",               // name
            List.of(MEDIA_TYPE), // supportedMediaTypes
            List.of("pdf"),      // supportedExtensions
            false,               // isRaster
            false,               // isVideo
            true);               // supportsTransparency

    private PDDocument doc;
    private MutableMetadata metadata;

    static {
        // This "fixes" the rendering of several PDFs. The docs also say that
        // this "may improve the performance of rendering PDFs on some systems
        // especially if there are a lot of images on a page."
        // See: https://github.com/cantaloupe-project/cantaloupe/issues/198
        // Also see: https://pdfbox.apache.org/2.0/getting-started.html
        System.setProperty("org.apache.pdfbox.rendering.UsePureJavaCMYKConversion", "true");
    }

    //region Plugin methods

    @Override
    public Set<String> getPluginConfigKeys() {
        return Set.of(DPI_CONFIG_KEY);
    }

    @Override
    public String getPluginName() {
        return getClass().getSimpleName();
    }

    @Override
    public void onApplicationStart() {}

    @Override
    public void onApplicationStop() {}

    @Override
    public void initializePlugin() {}

    //endregion
    //region Decoder methods

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            IOUtils.closeQuietly(doc);
            doc      = null;
            metadata = null;
        }
    }

    @Override
    public Format detectFormat() throws IOException {
        boolean close = false;
        ImageInputStream is = null;
        try {
            if (imageFile != null) {
                is = new PathImageInputStream(imageFile);
                close = true;
            } else {
                is = inputStream;
            }
            byte[] magicBytes = new byte[PDF_SIGNATURE.length];
            is.seek(0);
            is.readFully(magicBytes);
            is.seek(0);
            if (ArrayUtils.startsWith(magicBytes, PDF_SIGNATURE)) {
                return FORMAT;
            }
        } finally {
            if (is != null && close) {
                is.close();
            }
        }
        return Format.UNKNOWN;
    }

    @Override
    public int getNumImages() throws IOException {
        readDocument();
        return doc.getNumberOfPages();
    }

    @Override
    public int getNumResolutions() {
        return 1;
    }

    @Override
    public Size getSize(int pageIndex) throws IOException {
        validateImageIndex(pageIndex);

        final float scale = getDPI() / 72f;

        // PDF doesn't have native dimensions, so figure out the dimensions at
        // the current DPI setting.
        final PDPage page         = doc.getPage(pageIndex);
        final PDRectangle cropBox = page.getCropBox();
        final float widthPt       = cropBox.getWidth();
        final float heightPt      = cropBox.getHeight();
        final int rotationAngle   = page.getRotation();

        int widthPx  = Math.round(widthPt * scale);
        int heightPx = Math.round(heightPt * scale);
        if (rotationAngle == 90 || rotationAngle == 270) {
            int tmp  = widthPx;
            //noinspection SuspiciousNameCombination
            widthPx  = heightPx;
            heightPx = tmp;
        }
        return new Size(widthPx, heightPx);
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Set.of(FORMAT);
    }

    @Override
    public Size getTileSize(int pageIndex) throws IOException {
        validateImageIndex(pageIndex);
        return getSize(pageIndex);
    }

    @Override
    public BufferedImage decode(int pageIndex,
                                Region region,
                                double[] scales,
                                ReductionFactor reductionFactor,
                                double[] diffScales,
                                Set<DecoderHint> decoderHints) throws IOException {
        decoderHints.add(DecoderHint.IGNORED_REGION);
        decoderHints.add(DecoderHint.IGNORED_SCALE);
        return readImage(pageIndex, reductionFactor);
    }

    private BufferedImage readImage(int pageIndex,
                                    ReductionFactor rf) throws IOException {
        validateImageIndex(pageIndex);
        double dpi = new RasterizationHelper(getDPI()).getDPI(rf.factor);
        PDFRenderer renderer = new PDFRenderer(doc);
        try {
            return renderer.renderImageWithDPI(pageIndex, (float) dpi);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Metadata readMetadata(int imageIndex) throws IOException {
        if (metadata != null) {
            return metadata;
        }
        validateImageIndex(imageIndex);
        readDocument();
        metadata = new MutableMetadata();
        { // Read the document's native metadata.
            PDDocumentInformation info = doc.getDocumentInformation();
            PDFNativeMetadata pdfMetadata = new PDFNativeMetadata();
            for (String key : info.getMetadataKeys()) {
                if (info.getPropertyStringValue(key) != null) {
                    pdfMetadata.put(key, info.getPropertyStringValue(key).toString());
                }
            }
            metadata.setNativeMetadata(pdfMetadata);
        }
        { // Read the document's XMP metadata.
            PDMetadata pdfMetadata = doc.getDocumentCatalog().getMetadata();
            if (pdfMetadata != null) {
                metadata.setXMP(pdfMetadata.toByteArray());
            }
        }
        return metadata;
    }

    //endregion
    //region Private methods

    private int getDPI() {
        Configuration config = Configuration.forApplication();
        return config.getInt(DPI_CONFIG_KEY, DEFAULT_DPI);
    }

    private void readDocument() throws IOException {
        if (doc != null) {
            return;
        }
        final Stopwatch watch = new Stopwatch();

        if (imageFile != null) {
            try {
                doc = Loader.loadPDF(
                        new RandomAccessReadMemoryMappedFile(imageFile));
            } catch (IOException e) {
                throw new SourceFormatException();
            }
        } else {
            ImageInputStream is;
            if (inputStream != null) {
                is = inputStream;
            } else {
                throw new IOException("No source set");
            }
            try {
                doc = Loader.loadPDF(new RandomAccessReadImageInputStream(is));
            } catch (IOException e) {
                throw new SourceFormatException();
            }
        }

        // Disable the document's cache of PDImageXObjects
        // See: https://pdfbox.apache.org/2.0/faq.html#outofmemoryerror
        // This cache has never proven to be a problem, but it's not needed.
        doc.setResourceCache(new DefaultResourceCache() {
            @Override
            public void put(COSObject indirect, PDXObject xobject) {
                // no-op
            }
        });
        LOGGER.trace("Loaded document in {}", watch);
    }

    private void validateImageIndex(int pageIndex) throws IOException {
        readDocument();
        if (pageIndex < 0 || pageIndex >= getNumImages()) {
            throw new IndexOutOfBoundsException();
        }
    }

}
