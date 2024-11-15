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

import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadView;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

final class RandomAccessReadImageInputStream implements RandomAccessRead {

    private final ImageInputStream wrappedStream;
    private boolean isClosed;

    RandomAccessReadImageInputStream(ImageInputStream iis) {
        this.wrappedStream = iis;
    }

    @Override
    public int read() throws IOException {
        return wrappedStream.read();
    }

    @Override
    public int read(byte[] bytes, int i, int i1) throws IOException {
        return wrappedStream.read(bytes, i, i1);
    }

    @Override
    public long getPosition() throws IOException {
        return wrappedStream.getStreamPosition();
    }

    @Override
    public void seek(long l) throws IOException {
        wrappedStream.seek(l);
    }

    @Override
    public long length() throws IOException {
        return wrappedStream.length();
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public boolean isEOF() throws IOException {
        return wrappedStream.getStreamPosition() >= wrappedStream.length();
    }

    @Override
    public RandomAccessReadView createView(long l, long l1) {
        return new RandomAccessReadView(this, l, l1);
    }

    @Override
    public void close() throws IOException {
        wrappedStream.close();
        this.isClosed = true;
    }

}
