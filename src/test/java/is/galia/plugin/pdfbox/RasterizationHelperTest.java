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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RasterizationHelperTest extends BaseTest {

    private static final double DELTA = 0.0000001;

    private RasterizationHelper instance;

    @BeforeEach
    public void setUp() {
        super.setUp();
        instance = new RasterizationHelper(150);
    }

    @Test
    void constructor() {
        instance = new RasterizationHelper(200);
        assertEquals(200, instance.baseDPI());
    }

    @Test
    void getDPI() {
        assertEquals(instance.baseDPI() / 2.0, instance.getDPI(1), DELTA);
        assertEquals(instance.baseDPI() / 4.0, instance.getDPI(2), DELTA);
    }

}