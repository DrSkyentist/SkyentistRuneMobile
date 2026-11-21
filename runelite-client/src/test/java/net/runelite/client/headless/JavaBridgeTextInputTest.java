/*
 * Copyright (c) 2026, Jordan Simpson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.headless;

import java.lang.reflect.Method;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.VarClientInt;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The keyboard-kind snapshot behind {@link JavaBridge#getTextInputKind()}: the shells raise a number pad for
 * amount prompts and the Grand Exchange quantity/price entry, and a text keyboard for everything else. The kind
 * must be non-zero exactly when {@link JavaBridge#isTextInputOpen()} is true, or a shell could raise a keyboard
 * of the wrong type, or none at all.
 */
public class JavaBridgeTextInputTest
{
	static
	{
		System.setProperty("runelite.bridge", "true");
	}

	private static final int NONE = JavaBridge.TEXT_INPUT_NONE;
	private static final int TEXT = JavaBridge.TEXT_INPUT_TEXT;
	private static final int NUMBER = JavaBridge.TEXT_INPUT_NUMBER;

	@Test
	public void amountPromptsAreNumeric()
	{
		for (int t : new int[]{7, 16, 19, 30})
		{
			assertEquals("INPUT_TYPE " + t, NUMBER, JavaBridge.classifyTextInput(t, -1, false));
		}
	}

	@Test
	public void otherPromptsAreText()
	{
		// Private message, name entry, item search, RuneLite chatbox inputs.
		for (int t : new int[]{6, 8, 11, 14, -2, -3})
		{
			assertEquals("INPUT_TYPE " + t, TEXT, JavaBridge.classifyTextInput(t, -1, false));
		}
	}

	@Test
	public void focusedFieldIsNumericOnlyInTheGrandExchangeOffer()
	{
		assertEquals(NUMBER, JavaBridge.classifyTextInput(0, InterfaceID.GE_OFFERS, false));
		assertEquals(TEXT, JavaBridge.classifyTextInput(0, InterfaceID.CHATBOX, false));
	}

	@Test
	public void chatboxPromptWinsOverAFocusedField()
	{
		assertEquals(TEXT, JavaBridge.classifyTextInput(11, InterfaceID.GE_OFFERS, false));
	}

	@Test
	public void loginFieldsAreTextAndNothingOpenIsNone()
	{
		assertEquals(TEXT, JavaBridge.classifyTextInput(0, -1, true));
		assertEquals(NONE, JavaBridge.classifyTextInput(0, -1, false));
	}

	@Test
	public void snapshotPublishesKindWithOpenState() throws Exception
	{
		final Client c = mock(Client.class);
		when(c.getGameState()).thenReturn(GameState.LOGGED_IN);

		when(c.getVarcIntValue(VarClientInt.INPUT_TYPE)).thenReturn(7);
		snapshot(c);
		assertTrue(JavaBridge.isTextInputOpen());
		assertEquals(NUMBER, JavaBridge.getTextInputKind());

		when(c.getVarcIntValue(VarClientInt.INPUT_TYPE)).thenReturn(0);
		final Widget field = mock(Widget.class);
		when(field.getId()).thenReturn(InterfaceID.GE_OFFERS << 16 | 3);
		when(c.getFocusedInputFieldWidget()).thenReturn(field);
		snapshot(c);
		assertTrue(JavaBridge.isTextInputOpen());
		assertEquals(NUMBER, JavaBridge.getTextInputKind());

		when(c.getFocusedInputFieldWidget()).thenReturn(null);
		snapshot(c);
		assertFalse(JavaBridge.isTextInputOpen());
		assertEquals(NONE, JavaBridge.getTextInputKind());
	}

	// ---- KB-1: the destination label (getTextInputTarget). Engine state names the target; an open window alone never does.

	private static int target(int inputType, int focusedGroup, boolean login, boolean loggedIn, boolean mapSearching, boolean bankOpen)
	{
		return JavaBridge.classifyTextTarget(inputType, focusedGroup, login, loggedIn, mapSearching, bankOpen);
	}

	@Test
	public void noPromptWhileLoggedInIsChat()
	{
		assertEquals(JavaBridge.TEXT_TARGET_CHAT, target(0, -1, false, true, false, false));
	}

	@Test
	public void anOpenBankWithoutItsSearchPromptIsStillChat()
	{
		// The handoff's caution: the bank being open must not by itself relabel the keyboard "Bank Search".
		assertEquals(JavaBridge.TEXT_TARGET_CHAT, target(0, -1, false, true, false, true));
	}

	@Test
	public void searchPromptIsBankSearchOnlyWithTheBankOpen()
	{
		assertEquals(JavaBridge.TEXT_TARGET_BANK_SEARCH, target(11, -1, false, true, false, true));
		assertEquals(JavaBridge.TEXT_TARGET_SEARCH, target(11, -1, false, true, false, false));
	}

	@Test
	public void mapSearchHasNoMessageLayerModeButIsNamed()
	{
		assertEquals(JavaBridge.TEXT_TARGET_MAP_SEARCH, target(0, -1, false, true, true, false));
		// A live message-layer prompt still outranks the map flag: keys go to the prompt.
		assertEquals(JavaBridge.TEXT_TARGET_AMOUNT, target(7, -1, false, true, true, false));
	}

	@Test
	public void amountsPrivateMessagesAndOtherPrompts()
	{
		for (int t : new int[]{7, 16, 19, 30})
		{
			assertEquals("INPUT_TYPE " + t, JavaBridge.TEXT_TARGET_AMOUNT, target(t, -1, false, true, false, true));
		}
		assertEquals(JavaBridge.TEXT_TARGET_PRIVATE_MESSAGE, target(6, -1, false, true, false, false));
		for (int t : new int[]{8, 14, -2, -3})
		{
			assertEquals("INPUT_TYPE " + t, JavaBridge.TEXT_TARGET_PROMPT, target(t, -1, false, true, false, false));
		}
		assertEquals(JavaBridge.TEXT_TARGET_AMOUNT, target(0, InterfaceID.GE_OFFERS, false, true, false, false));
		assertEquals(JavaBridge.TEXT_TARGET_PROMPT, target(0, InterfaceID.CHATBOX, false, true, false, false));
	}

	@Test
	public void loginFormAndLoggedOut()
	{
		assertEquals(JavaBridge.TEXT_TARGET_LOGIN, target(0, -1, true, false, false, false));
		assertEquals(JavaBridge.TEXT_TARGET_NONE, target(0, -1, false, false, false, false));
	}

	@Test
	public void kindStaysConsistentWithTarget()
	{
		// The target is labelling only; the keyboard kind (and so the auto-raise) is unchanged by it. Every
		// non-NONE kind must map to a non-NONE target for the same inputs (logged in).
		for (int t : new int[]{0, 6, 7, 8, 11, 14, 16, 19, 30, -2, -3})
		{
			final int kind = JavaBridge.classifyTextInput(t, -1, false);
			final int tgt = target(t, -1, false, true, false, false);
			if (kind != NONE)
			{
				assertTrue("INPUT_TYPE " + t, tgt != JavaBridge.TEXT_TARGET_NONE && tgt != JavaBridge.TEXT_TARGET_CHAT);
			}
		}
	}

	@Test
	public void snapshotPublishesBankSearchAndMapSearchFromEngineState() throws Exception
	{
		final Client c = mock(Client.class);
		when(c.getGameState()).thenReturn(GameState.LOGGED_IN);
		final Widget shown = mock(Widget.class);
		when(shown.isHidden()).thenReturn(false);

		// Bank open, no prompt: chat.
		when(c.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(shown);
		when(c.getVarcIntValue(VarClientInt.INPUT_TYPE)).thenReturn(0);
		snapshot(c);
		assertEquals(JavaBridge.TEXT_TARGET_CHAT, JavaBridge.getTextInputTarget());

		// The bank's search prompt opens.
		when(c.getVarcIntValue(VarClientInt.INPUT_TYPE)).thenReturn(11);
		snapshot(c);
		assertEquals(JavaBridge.TEXT_TARGET_BANK_SEARCH, JavaBridge.getTextInputTarget());
		assertEquals(TEXT, JavaBridge.getTextInputKind());

		// Search closed, bank closed: back to chat, no stale "Bank Search".
		when(c.getVarcIntValue(VarClientInt.INPUT_TYPE)).thenReturn(0);
		when(c.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(null);
		snapshot(c);
		assertEquals(JavaBridge.TEXT_TARGET_CHAT, JavaBridge.getTextInputTarget());

		// World map open and searching (no MESLAYERMODE): map search. Kind/open state are untouched by it.
		when(c.getWidget(InterfaceID.Worldmap.MAP_CONTAINER)).thenReturn(shown);
		when(c.getVarcIntValue(net.runelite.api.gameval.VarClientID.WORLDMAP_SEARCHING)).thenReturn(1);
		snapshot(c);
		assertEquals(JavaBridge.TEXT_TARGET_MAP_SEARCH, JavaBridge.getTextInputTarget());
		assertFalse(JavaBridge.isTextInputOpen());
		assertEquals(NONE, JavaBridge.getTextInputKind());

		// A stale search flag with the map closed is not a map search.
		when(c.getWidget(InterfaceID.Worldmap.MAP_CONTAINER)).thenReturn(null);
		snapshot(c);
		assertEquals(JavaBridge.TEXT_TARGET_CHAT, JavaBridge.getTextInputTarget());
	}

	private static void snapshot(Client c) throws Exception
	{
		final Method m = JavaBridge.class.getDeclaredMethod("snapshotTextInputOpen", Client.class);
		m.setAccessible(true);
		m.invoke(null, c);
	}
}
