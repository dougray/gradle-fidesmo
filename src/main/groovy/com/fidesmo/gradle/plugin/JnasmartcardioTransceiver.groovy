/*
 * Copyright 2014 Fidesmo AB
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

package com.fidesmo.gradle.plugin

import com.fidesmo.sec.transceivers.AbstractTransceiver

import javax.smartcardio.*
import jnasmartcardio.Smartcardio

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class JnasmartcardioTransceiver extends AbstractTransceiver {

    Logger logger = LoggerFactory.getLogger(this.getClass())

    Card card
    Boolean open

    public byte[] open() {
        TerminalFactory factory = TerminalFactory.getInstance("PC/SC", null, new Smartcardio())
        List<CardTerminal> terminalsWithCard = factory.terminals().list(CardTerminals.State.CARD_PRESENT)
        if (terminalsWithCard.size() == 0) {
            if (factory.terminals().list().size() == 0) {
                throw new Exception('No terminals found')
            } else {
                throw new Exception('No terminal with card found')
            }
        }
        CardTerminal terminal = terminalsWithCard.first()
        logger.info("Using terminal '${terminal.name}' to connect to fidesmo card")
        open = true
        card = terminal.connect('*')
        card.ATR.bytes
    }

    public void close() {
        open = false
        card.disconnect(false)
    }

    public byte[] transceive(byte[] command) {
        CardChannel cardChannel = card.basicChannel
        ResponseAPDU responseApdu = cardChannel.transmit(new CommandAPDU(command))
        responseApdu.bytes
    }
}
