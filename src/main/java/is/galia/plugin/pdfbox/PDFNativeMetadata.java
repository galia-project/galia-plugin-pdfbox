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

import com.fasterxml.jackson.annotation.JsonValue;
import is.galia.image.NativeMetadata;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class PDFNativeMetadata implements NativeMetadata {

    @JsonValue
    private final Map<String,String> backingMap = new TreeMap<>(); // ordered

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof PDFNativeMetadata other) {
            return backingMap.equals(other.backingMap);
        }
        return super.equals(obj);
    }

    public String get(String key) {
        return backingMap.get(key);
    }

    @Override
    public int hashCode() {
        return backingMap.hashCode();
    }

    public void put(String key, String value) {
        backingMap.put(key, value);
    }

    @Override
    public Map<String, String> toMap() {
        return Collections.unmodifiableMap(backingMap);
    }

    @Override
    public String toString() {
        if (!backingMap.isEmpty()) {
            return backingMap.entrySet()
                    .stream()
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .collect(Collectors.joining(";"));
        }
        return "(empty)";
    }

}
