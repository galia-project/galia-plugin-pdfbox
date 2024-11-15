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

/**
 * Assists in rasterization of vector source images.
 */
record RasterizationHelper(int baseDPI) {

    /**
     * @return DPI at 1x scale.
     */
    @Override
    public int baseDPI() {
        return baseDPI;
    }

    /**
     * @return DPI appropriate for the given reduction factor.
     */
    public double getDPI(int reductionFactor) {
        double rfDPI = baseDPI;
        // Decrease the DPI if the reduction factor is positive.
        for (int i = 0; i < reductionFactor; i++) {
            rfDPI /= 2.0;
        }
        // Increase the DPI if the reduction factor is negative.
        for (int i = 0; i > reductionFactor; i--) {
            rfDPI *= 2.0;
        }
        return Math.min(rfDPI, baseDPI);
    }

}
