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

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;

import nordpol.IsoCard;
import nordpol.OnCardErrorListener;

public class SmartcardioCard implements IsoCard {

    private Card card;
    private boolean openFlag = false;
    private int timeout = 15;
    private List<OnCardErrorListener> listeners = new ArrayList<OnCardErrorListener>();

    public SmartcardioCard(Card card) {
        this.card = card;
    }

    public void addOnCardErrorListener(OnCardErrorListener listener) {
        listeners.add(listener);
    }

    public void removeOnCardErrorListener(OnCardErrorListener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return openFlag;
    }

    public void connect() {
        openFlag = true;
    }

    public void close() throws IOException {
        try {
            card.disconnect(false);
            openFlag = false;
        } catch (CardException e) {
            throw new IOException("Couldn't close card", e);
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int t) {
        timeout = t;
    }

    public int getMaxTransceiveLength() {
        return 261;
    }

    public byte[] transceive(byte[] command) throws IOException {
        try {
            return card
                .getBasicChannel()
                .transmit(new CommandAPDU(command))
                .getBytes();
        } catch (CardException e) {
            throw new IOException("Couldn't close card", e);
        }
    }

    public List<byte[]> transceive(List<byte[]> commands) throws IOException {
        List<byte[]> responses = new ArrayList<byte[]>(commands.size());
        for (byte[] command: commands) {
            responses.add(transceive(command));
        }
        return responses;
    }

}
