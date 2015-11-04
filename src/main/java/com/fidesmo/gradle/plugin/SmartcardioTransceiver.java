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
import java.security.NoSuchAlgorithmException;
import javax.smartcardio.TerminalFactory;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CardException;

import jnasmartcardio.Smartcardio;
import rx.Observable;

import com.fidesmo.sec.transceivers.Transceiver;
import nordpol.IsoCard;


/** Transceiver implementation using the javax.smartcardio package.
 *
 * This transceiver uses the jnasmartcardio package instead of the
 * default implementation.
 */
public class SmartcardioTransceiver implements Transceiver {

    public Observable<IsoCard> getCard() {
        try {
            TerminalFactory factory =
                TerminalFactory.getInstance("PC/SC",null, new Smartcardio());
            List<CardTerminal> terminalsWithCard =
                factory.terminals().list(CardTerminals.State.CARD_PRESENT);

            if (terminalsWithCard.size() == 0) {
                if (factory.terminals().list().size() == 0) {
                    return Observable.error(new IOException("No terminals found"));
                } else {
                    return Observable.error(new IOException("No card found"));
                }
            }

            return Observable
                .just((IsoCard) new LoggingCard(new SmartcardioCard(terminalsWithCard
                                                                    .get(0)
                                                                    .connect("*"))));
        } catch (NoSuchAlgorithmException e) {
            return Observable.error(new IOException("Error with jnassmartcardio", e));
        } catch (CardException e) {
            return Observable.error(new IOException("Unable to get Smartcard", e));
        }
  }
}
