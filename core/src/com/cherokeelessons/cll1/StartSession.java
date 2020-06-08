package com.cherokeelessons.cll1;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.badlogic.gdx.utils.Logger;
import com.cherokeelessons.cll1.models.CardData;
import com.cherokeelessons.cll1.models.GameCard;
import com.cherokeelessons.cll1.screens.LearningSession;
import com.cherokeelessons.deck.CardStats;
import com.cherokeelessons.deck.Deck;
import com.cherokeelessons.deck.ICard;
import com.cherokeelessons.util.SlotFolder;

public class StartSession implements Runnable {

	/**
	 * Eight-bit UCS Transformation Format
	 */
	public static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final String ACTIVE_CARDS = CLL1.ACTIVE_CARDS;

	private final Logger log = new Logger(this.getClass().getSimpleName());
	private final AbstractGame game;
	private final int session;
	private final Json json = new Json();

	public StartSession(final AbstractGame game, final int session) {
		this.game = game;
		this.session = session;
		log.setLevel(Logger.INFO);
	}

	@Override
	public void run() {
		log.info("StartSession#run");
		final FileHandle activeCardsJson = SlotFolder.getSlotFolder(session).child(ACTIVE_CARDS);
		String tmp;
		try {
			tmp = activeCardsJson.readString(UTF_8.name());
		} catch (final Exception e) {
			tmp = "";
		}
		log.info("StartSession#file loaded");
		final String[] jsonCardsStats = tmp.split("\n");
		/*
		 * Always create a fresh deck by copying (cloning) cards from the CSV master
		 * deck. If we have stats that don't match a card in the fresh deck they will be
		 * ignored and lost at next save. The match up is done by "card id" which is the
		 * minimal amount of uniqueness to match up as generated by the CardData object.
		 * Being sure to set all stats to "never shown/correct".
		 */
		final Deck<CardData> masterDeck = new Deck<CardData>();
		for (final GameCard card : ((CLL1) game).cards) {
			final ICard<CardData> copy = card.copy();
			copy.resetStats();
			copy.resetTriesRemaining(CardData.MAX_TRIES);
			copy.getCardStats().setPimsleurSlot(0);
			masterDeck.add(copy);
		}
		log.info("StartSession#master deck created");
		if (Gdx.app.getType().equals(ApplicationType.Desktop)) {
			masterDeck.shuffleThenSortIntoPrefixedGroups(CardData.SORT_KEY_LENGTH);
			SlotFolder.getDeckSlot().mkdirs();
			final List<String[]> sortedCardIds = new ArrayList<String[]>();
			for (final ICard<CardData> card : masterDeck.getCards()) {
				sortedCardIds.add(new String[] { card.id(), card.sortKey() });
			}
			final Json jsonx = new Json(OutputType.json);
			jsonx.setTypeName(null);
			jsonx.setUsePrototypes(false);
			SlotFolder.getDeckSlot().child("master-deck-card-ids.json").writeString(jsonx.prettyPrint(sortedCardIds),
					false, UTF_8.name());
			log.info("StartSession#master deck debug file created");
		}

		/*
		 * Shove master deck cards into a Map<> for fast lookup by id.
		 */

		final Map<String, ICard<CardData>> cardLookupMap = new HashMap<String, ICard<CardData>>();
		for (final ICard<CardData> card : masterDeck.getCards()) {
			cardLookupMap.put(card.id(), card);
		}

		final Deck<CardData> activeDeck = new Deck<CardData>();
		/*
		 * The json card stats data aren't stored directly as a single json object in a
		 * list to reduce memory requirements for serialization and deserialization.
		 * Each valid set of stats indicates a card already "in play". Update each such
		 * card with the saved stats and move each one into the "active deck".
		 */
		for (final String jsonCardStats : jsonCardsStats) {
			if (jsonCardStats.trim().isEmpty()) {
				continue;
			}
			if (!jsonCardStats.contains("\t")) {
				continue;
			}
			final String[] txtStats = jsonCardStats.split("\t");
			if (txtStats[0].trim().isEmpty()) {
				continue;
			}
			if (txtStats[1].trim().isEmpty()) {
				continue;
			}
			final String id = txtStats[0];
			final ICard<CardData> card = cardLookupMap.get(id);
			/*
			 * stats refers to a card that no longer exists...
			 */
			if (card == null) {
				log.error("No matching card found for: '" + id + "'");
				continue;
			}
			final CardStats stats = json.fromJson(CardStats.class, txtStats[1]);
			card.setCardStats(stats);
			/*
			 * move this "in play" card into the active deck. Being sure to set all stats to
			 * "never shown/correct".
			 */
			card.resetStats();
			card.resetTriesRemaining(CardData.MAX_TRIES);
			card.getCardStats().setPimsleurSlot(0);
			activeDeck.add(card);
		}
		log.info("StartSession#active deck created");
		game.setScreen(new LearningSession(game, session, masterDeck, activeDeck));
	}
}