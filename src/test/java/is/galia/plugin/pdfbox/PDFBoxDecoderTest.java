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

import is.galia.codec.DecoderHint;
import is.galia.codec.SourceFormatException;
import is.galia.image.MediaType;
import is.galia.image.Size;
import is.galia.plugin.pdfbox.test.TestUtils;
import is.galia.config.Configuration;
import is.galia.image.Format;
import is.galia.image.Metadata;
import is.galia.image.Region;
import is.galia.image.ReductionFactor;
import is.galia.stream.PathImageInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PDFBoxDecoderTest extends BaseTest {

    private PDFBoxDecoder instance;

    //////////////////////////////////////////////////////////////////////////
    //region Setup/Teardown
    //////////////////////////////////////////////////////////////////////////

    @BeforeAll
    public static void beforeClass() {
        try (PDFBoxDecoder decoder = new PDFBoxDecoder()) {
            decoder.onApplicationStart();
        }
    }

    @Override
    public void setUp() {
        Configuration config = Configuration.forApplication();
        config.clearProperty(PDFBoxDecoder.DPI_CONFIG_KEY);

        instance = new PDFBoxDecoder();
        instance.initializePlugin();
        instance.setSource(FIXTURE);
    }

    @Override
    public void tearDown() {
        instance.close();
    }

    //////////////////////////////////////////////////////////////////////////
    //endregion
    //region Source tests
    //
    // These test only read(int)--it shouldn't be necessary to test any others
    // because they all exercise the same code path internally.
    //////////////////////////////////////////////////////////////////////////

    @Test
    void decodeFromFile() throws Exception {
        instance.setSource(FIXTURE);
        BufferedImage image = instance.decode(0);
        assertEquals(208, image.getWidth());
        assertEquals(183, image.getHeight());
    }

    @Test
    void decodeFromImageInputStream() throws Exception {
        instance.setSource(new PathImageInputStream(FIXTURE));
        BufferedImage image = instance.decode(0);
        assertEquals(208, image.getWidth());
        assertEquals(183, image.getHeight());
    }

    @Test
    void decodeWithoutSourceSet() {
        instance.setSource((Path) null);
        instance.setSource((ImageInputStream) null);
        assertThrows(IOException.class, () -> instance.decode(0));
    }

    @Test
    void decodeWithNonPDF() {
        instance.setSource(TestUtils.getFixture("jpg/jpg.jpg"));
        assertThrows(SourceFormatException.class, () -> instance.decode(0));
    }

    //////////////////////////////////////////////////////////////////////////
    //endregion
    //region Plugin Tests
    //////////////////////////////////////////////////////////////////////////

    @Test
    void getPluginConfigKeys() {
        Set<String> keys = instance.getPluginConfigKeys();
        assertFalse(keys.isEmpty());
    }

    @Test
    void getPluginName() {
        assertEquals(PDFBoxDecoder.class.getSimpleName(),
                instance.getPluginName());
    }

    //////////////////////////////////////////////////////////////////////////
    //endregion
    //region Decoder tests
    //////////////////////////////////////////////////////////////////////////

    /* detectFormat() */

    @Test
    void detectFormatWithSupportedMagicBytes() throws Exception {
        assertEquals(Format.get("pdf"), instance.detectFormat());
    }

    @Test
    void detectFormatWithUnsupportedMagicBytes() throws Exception {
        instance.setSource(TestUtils.getFixture("jpg.jpg"));
        assertEquals(Format.UNKNOWN, instance.detectFormat());
    }

    /* getNumImages() */

    @Test
    void getNumImages() throws Exception {
        instance.setSource(TestUtils.getFixture("2page.pdf"));
        assertEquals(2, instance.getNumImages());
    }

    /* getNumResolutions() */

    @Test
    void getNumResolutions() {
        assertEquals(1, instance.getNumResolutions());
    }

    /* getSize() */

    @Test
    void getSize() throws Exception {
        Size actual = instance.getSize(0);
        assertEquals(new Size(208, 183), actual);
    }

    @Test
    void getSizeWithIllegalPageIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.getSize(1));
    }

    /* getSupportedFormats() */

    @Test
    void getSupportedFormats() {
        Set<Format> formats = instance.getSupportedFormats();
        assertEquals(1, formats.size());
        Format expected = new Format(
                "pdf",                                        // key
                "PDF",                                        // name
                List.of(new MediaType("application", "pdf")), // supportedMediaTypes
                List.of("pdf"),                               // supportedExtensions
                false,                                        // isRaster
                false,                                        // isVideo
                true);                                        // supportsTransparency
        Format actual = formats.stream().findAny().orElseThrow();
        assertEquals(expected, actual);
    }

    /* getTileSize() */

    @Test
    void getTileSize() throws Exception {
        Size actual = instance.getTileSize(0);
        assertEquals(new Size(208, 183), actual);
    }

    @Test
    void getTileSizeWithIllegalPageIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.getTileSize(1));
    }

    /* read(int) */

    @Test
    void decode1() throws Exception {
        BufferedImage image = instance.decode(0);
        assertEquals(208, image.getWidth());
        assertEquals(183, image.getHeight());
    }

    @Test
    void decode1RespectsDPISetting() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(PDFBoxDecoder.DPI_CONFIG_KEY,
                PDFBoxDecoder.DEFAULT_DPI * 2);
        BufferedImage image = instance.decode(0);
        assertEquals(208 * 2, image.getWidth());
        assertEquals(183 * 2, image.getHeight());
    }

    @Test
    void decode1WithIllegalPageIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.decode(1));
    }

    /* read(int, Crop, Scale, ScaleConstraint, ReductionFactor,
            Set<DecoderHint>) */

    @Test
    void decode2() throws Exception {
        Region region                   = new Region(0, 0, 200, 100);
        double[] scales                 = { 0.5, 0.5 };
        double[] diffScales             = new double[2];
        ReductionFactor reductionFactor = new ReductionFactor();
        Set<DecoderHint> decoderHints   = new HashSet<>();
        BufferedImage image = instance.decode(
                0, region, scales, reductionFactor, diffScales, decoderHints);
        assertEquals(208, image.getWidth());
        assertEquals(183, image.getHeight());
        assertTrue(decoderHints.contains(DecoderHint.IGNORED_REGION));
        assertTrue(decoderHints.contains(DecoderHint.IGNORED_SCALE));
    }

    @Test
    void decode2RespectsDPISetting() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(PDFBoxDecoder.DPI_CONFIG_KEY,
                PDFBoxDecoder.DEFAULT_DPI * 2);

        Region region                   = new Region(0, 0, 9999, 9999);
        double[] scales                 = { 1, 1 };
        double[] diffScales             = new double[2];
        ReductionFactor reductionFactor = new ReductionFactor();
        Set<DecoderHint> decoderHints   = new HashSet<>();
        BufferedImage image = instance.decode(
                0, region, scales, reductionFactor, diffScales, decoderHints);
        assertEquals(416, image.getWidth());
        assertEquals(366, image.getHeight());
    }

    @Test
    void decode2WithIllegalPageIndex() {
        Region region                = new Region(0, 0, 9999, 9999);
        double[] scales                 = { 1, 1 };
        double[] diffScales             = new double[2];
        ReductionFactor reductionFactor = new ReductionFactor();
        Set<DecoderHint> decoderHints   = new HashSet<>();
        assertThrows(IndexOutOfBoundsException.class, () ->
                instance.decode(1, region, scales, reductionFactor,
                        diffScales, decoderHints));
    }

    /* readMetadata() */

    @Test
    void decodeMetadata() throws Exception {
        instance.setSource(TestUtils.getFixture("xmp.pdf"));
        Metadata metadata = instance.readMetadata(0);
        // check native metadata
        PDFNativeMetadata md = (PDFNativeMetadata) metadata.getNativeMetadata().orElseThrow();
        assertNotNull(md.get("Producer"));
        // check XMP
        String xmp = metadata.getXMP().orElseThrow();
        assertTrue(xmp.startsWith("<rdf:RDF"));
    }

    @Test
    void decodeMetadataWithIllegalImageIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.readMetadata(9999));
    }

    /* readSequence() */

    @Test
    void decodeSequence() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.decodeSequence());
    }

}
