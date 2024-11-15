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

import is.galia.plugin.pdfbox.test.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Path;

public abstract class BaseTest {

    protected static final String IMAGE = "1page.pdf";
    protected static final Path FIXTURE = TestUtils.getFixture(IMAGE);

    @BeforeAll
    public static void beforeAll() {
        // Suppress a Dock icon and annoying Space transition in full-screen
        // mode in macOS.
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

}
