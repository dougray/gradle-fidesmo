/*
 * Copyright 2015 Fidesmo AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.fidesmo.gradle.plugin;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import com.fidesmo.sec.utils.Hex;
import nordpol.IsoCard;
import nordpol.OnCardErrorListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wrap an IsoCard so that all transmitted APDUs are logged */

public class LoggingCard implements IsoCard {

    IsoCard underlying;
    Logger logger;

    public LoggingCard(IsoCard underlying) {
        this.underlying = underlying;
        this.logger = LoggerFactory.getLogger(LoggingCard.class);
    }

    public void addOnCardErrorListener(OnCardErrorListener listener) {
        underlying.addOnCardErrorListener(listener);
    }

    public void removeOnCardErrorListener(OnCardErrorListener listener) {
        underlying.removeOnCardErrorListener(listener);
    }

    public boolean isConnected() {
        return underlying.isConnected();
    }

    public void connect() throws IOException {
        underlying.connect();
    }

    public void close() throws IOException {
        underlying.close();
    }

    public int getTimeout() {
        return underlying.getTimeout();
    }

    public void setTimeout(int t) {
        underlying.setTimeout(t);
    }

    public int getMaxTransceiveLength() throws IOException {
        return underlying.getMaxTransceiveLength();
    }

    public byte[] transceive(byte[] command) throws IOException {
        logger.info("==> ApduCommand(" + Hex.encodeHex(command) + ")");
        byte[] response = underlying.transceive(command);
        logger.info("<== ApduResponse(" + Hex.encodeHex(response) + ")");
        return response;
    }

    public List<byte[]> transceive(List<byte[]> commands) throws IOException {
        List<byte[]> responses = new ArrayList<byte[]>(commands.size());
        for (byte[] command: commands) {
            responses.add(transceive(command));
        }
        return responses;
    }
}
