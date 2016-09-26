package com.cherokeelessons.cll2ev1.screens;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Scaling;
import com.cherokeelessons.cll2ev1.AbstractGame;
import com.cherokeelessons.cll2ev1.CLL2EV1;
import com.cherokeelessons.cll2ev1.models.CardData;
import com.cherokeelessons.deck.CardStats;
import com.cherokeelessons.deck.CardUtils;
import com.cherokeelessons.deck.Deck;
import com.cherokeelessons.deck.DeckStats;
import com.cherokeelessons.deck.ICard;
import com.cherokeelessons.util.SlotFolder;

public class LearningSession extends AbstractScreen implements Screen {
	private static final String DING = "audio/ding.mp3";
	private static final String BUZZER = "audio/buzzer2.mp3";
	private static final String XMARK = "images/2716_white.png";
	private static final String CHECKMARK = "images/2714_white.png";
	private static final String IMAGES_OVERLAY = "images/overlay-border.png";
	private static final String IMAGES_BACKDROP = "images/white-backdrop.png";
	private final int session;
	private final Deck<CardData> masterDeck;
	private final Deck<CardData> activeDeck;
	private final Deck<CardData> discardsDeck = new Deck<CardData>();
	private final Deck<CardData> completedDeck = new Deck<CardData>();

	public LearningSession(AbstractGame game, int session, Deck<CardData> masterDeck, Deck<CardData> activeDeck) {
		super(game);
		this.session = session;
		this.masterDeck = masterDeck;
		this.activeDeck = activeDeck;
		setBackdrop(CLL2EV1.BACKDROP);
		setSkin(CLL2EV1.SKIN);

		log("Session: " + session);
		log("Master Deck Size: " + masterDeck.size());
		log("Active Deck Size: " + activeDeck.size());
		log("First Time: " + (activeDeck.size() == 0));

		if (activeDeck.size() == 0) {
			stage.addAction(actionFirstTime());
		} else {
			stage.addAction(actionLoadNextChallenge());
		}

		stage.addAction(actionUpdateTimeLeft());

		lblCountdown = new Label("0:00", skin);
		lblCountdown.setFontScale(.75f);

		challengeText = new Label("", skin);
		choice1 = new Stack();
		choice2 = new Stack();

		choice1.setTouchable(Touchable.childrenOnly);
		choice2.setTouchable(Touchable.childrenOnly);

		Gdx.app.postRunnable(init1);
		Gdx.app.postRunnable(init2);

		assets.load(CHECKMARK, Texture.class);
		assets.load(XMARK, Texture.class);
		assets.finishLoadingAsset(CHECKMARK);
		assets.finishLoadingAsset(XMARK);
		checkmark = assets.get(CHECKMARK, Texture.class);
		xmark = assets.get(XMARK, Texture.class);
		imgCheckmark = new Image(checkmark);
		imgCheckmark.setScaling(Scaling.fit);
		imgCheckmark.setColor(new Color(Color.FOREST));
		imgCheckmark.getColor().a = .75f;
		imgXmark = new Image(xmark);
		imgXmark.setScaling(Scaling.fit);
		imgXmark.setColor(new Color(Color.FIREBRICK));
		imgXmark.getColor().a = .75f;

		assets.load(BUZZER, Sound.class);
		assets.load(DING, Sound.class);
	}

	private float maxTime_secs = 5.0f * 60f;

	private void updateTimeleft() {
		float timeleft_secs = maxTime_secs - totalElapsed;
		if (timeleft_secs < 0f) {
			timeleft_secs = 0f;
		}
		int minutes = (int) (timeleft_secs / 60f);
		int seconds = ((int) (timeleft_secs)) - 60 * minutes;
		String tmp = minutes + ":" + ((seconds < 10) ? "0" : "") + seconds;
		lblCountdown.setText(tmp);
		lblCountdown.pack();
	}

	private Action actionUpdateTimeLeft() {
		return Actions.sequence(Actions.delay(.1f), Actions.run(new Runnable() {
			@Override
			public void run() {
				stage.addAction(actionUpdateTimeLeft());
				updateTimeleft();
			}
		}));
	}

	private Texture checkmark;
	private Texture xmark;

	private Image imgCheckmark;
	private Image imgXmark;

	private Table uiTable;
	private Table gameTable;
	private Table challengeTable;
	private Table answersTable;

	private Music challengeAudio;

	private final Label challengeText;
	private final Stack choice1;
	private final Stack choice2;
	protected Runnable init1 = new Runnable() {
		@Override
		public void run() {
			prepDecks();
		}
	};
	protected Runnable init2 = new Runnable() {
		@Override
		public void run() {
			initUi();
		}
	};

//	private float sinceLastDeckShuffle_elapsed = 0f;
	private String previousCardId = null;
	private String activeAudioFile;
	private String activeImageFile1;
	private String activeImageFile2;
	private int correct = 1;
	private ICard<CardData> activeCard;
	private CardData activeCardData;
	private CardStats activeCardStats;

	@Override
	protected void act(float delta) {
//		sinceLastDeckShuffle_elapsed += delta;
	}

	private void loadNextChallenge() {
		ICard<CardData> card;
		card = getNextCard();
		CardStats cardStats = card.getCardStats();
		activeCard = card;
		activeCardData = card.getData();
		activeCardStats = card.getCardStats();
		loadNewChallengeImages(activeCardData);
		loadNewChallengeAudio(activeCardData.nextRandomAudioFile());
		// reset for each card being displayed
		currentElapsed = 0f;
	}

	private void showFinalStats(){
		log("SHOWING FINAL STATS");
		userPause();
		pausedStage.getRoot().clearChildren();
		Dialog finalStats = new Dialog("FINAL STATS", skin) {
			@Override
			protected void result(Object object) {
				userResume();
				game.previousScreen();
			}
		};
		finalStats.setModal(true);
		finalStats.setFillParent(true);
		finalStats.getTitleLabel().setAlignment(Align.center);
		TextButton howa = new TextButton("ᎰᏩ", skin);
		howa.setDisabled(true);
		finalStats.button(howa);
		
		DeckStats stats = DeckStats.calculateStats(activeDeck);
		StringBuilder txt = new StringBuilder();
		txt.append("LEVEL: ");
		txt.append(stats.level.getEnglish());
		txt.append("\n");
		txt.append("Active cards: ");
		txt.append(stats.activeCards);
		txt.append("\n");
		txt.append("Score: ");
		txt.append(stats.lastScore);
		txt.append("\n");
		txt.append("Proficiency: ");
		txt.append(stats.proficiency);
		txt.append("%");

		Table contentTable = finalStats.getContentTable();
		contentTable.row();
		Table noticeTable = new Table(skin);
		noticeTable.defaults().expand().fill();
		contentTable.add(noticeTable).expand().fill();
		Label message = new Label(txt.toString(), skin);
		message.setAlignment(Align.center);
		message.setWrap(true);
		message.setFontScale(1.2f);
		noticeTable.add(message);
		finalStats.show(pausedStage);
		
		pausedStage.addAction(actionSaveActiveDeck(howa));
	}
	
	private void endSessionCleanup(){
		/*
		 * Fist combine completed, discards, and active into a single deck
		 * before doing stats and also for saving to storage.
		 */
		while (completedDeck.hasCards()) {
			activeDeck.add(completedDeck.topCard());
		}
		while (discardsDeck.hasCards()) {
			activeDeck.add(discardsDeck.topCard());
		}
	}
	
	private Action actionShowCompletedDialog(){
		return Actions.run(new Runnable() {
			@Override
			public void run() {
				endSessionCleanup();
				showFinalStats();
			}
		});
	}
	
	private Action actionSaveActiveDeck(final Button howa){
		return Actions.run(new Runnable() {
			@Override
			public void run() {
				saveActiveDeck();
				howa.setDisabled(false);
			}
		});
	}
	
	private void saveActiveDeck(){
		Json json = new Json();
		json.setUsePrototypes(false);
		FileHandle slot = SlotFolder.getSlotFolder(session);
		slot.mkdirs();
		
		FileHandle fh_activeCards_tmp = slot.child(CLL2EV1.ACTIVE_CARDS+".tmp");
		FileHandle fh_activeCards = slot.child(CLL2EV1.ACTIVE_CARDS);
		activeDeck.shuffleThenSortByNextSession();
		StringBuilder sbActiveCards=new StringBuilder();
		for (ICard<CardData> card: activeDeck.getCards()) {
			sbActiveCards.append(card.id());
			sbActiveCards.append("\t");
			sbActiveCards.append(json.toJson(card.getCardStats()));
			sbActiveCards.append("\n");
		}
		fh_activeCards_tmp.writeString(sbActiveCards.toString(), false, StandardCharsets.UTF_8.name());
		fh_activeCards_tmp.moveTo(fh_activeCards);
		
		FileHandle fh_deckstats_tmp = slot.child(CLL2EV1.DECKSTATS+".tmp");
		FileHandle fh_deckstats = slot.child(CLL2EV1.DECKSTATS);
		json.toJson(DeckStats.calculateStats(activeDeck), fh_deckstats_tmp);
		fh_deckstats_tmp.moveTo(fh_deckstats);
	}

	private Action actionLoadNextChallenge() {
		return Actions.sequence(Actions.delay(1.5f), Actions.run(new Runnable() {
			public void run() {
				// 5-minute check
				if (totalElapsed > maxTime_secs) {
					log("=== SESSION TIME UP!");
					pausedStage.addAction(actionShowCompletedDialog());
					userPause();
					return;
				}
				loadNextChallenge();
			}
		}));
	}

	private void loadNewChallengeImages(final CardData cdata) {
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				challengeText.setText(cdata.text);

				choice1.clear();
				choice2.clear();
				discardImageFor(activeImageFile1);
				discardImageFor(activeImageFile2);

				Set<String> previousImages = new HashSet<String>();
				previousImages.add(activeImageFile1);
				previousImages.add(activeImageFile2);
				if (isHeads()) {
					correct = 1;
					activeImageFile1 = cdata.nextRandomImageFile();
					if (previousImages.contains(activeImageFile1)) {
						activeImageFile1 = cdata.nextRandomImageFile();
					}
					activeImageFile2 = cdata.nextRandomWrongImageFile();
					if (previousImages.contains(activeImageFile2)) {
						activeImageFile2 = cdata.nextRandomWrongImageFile();
					}
				} else {
					correct = 2;
					activeImageFile1 = cdata.nextRandomWrongImageFile();
					if (previousImages.contains(activeImageFile1)) {
						activeImageFile1 = cdata.nextRandomWrongImageFile();
					}
					activeImageFile2 = cdata.nextRandomImageFile();
					if (previousImages.contains(activeImageFile2)) {
						activeImageFile2 = cdata.nextRandomImageFile();
					}
				}

				choice1.clear();
				choice2.clear();
				choice1.setTouchable(Touchable.childrenOnly);
				choice2.setTouchable(Touchable.childrenOnly);
				for (Image img : getImageFor(activeImageFile1)) {
					choice1.add(img);
					img.addListener(maybe1);
				}
				for (Image img : getImageFor(activeImageFile2)) {
					choice2.add(img);
					img.addListener(maybe2);
				}
			}
		});
	}

	private void loadNewChallengeAudio(final String newActiveAudioFile) {
		Gdx.app.postRunnable(new Runnable() {
			public void run() {
				if (challengeAudio != null) {
					challengeAudio.stop();
				}
				if (activeAudioFile != null) {
					if (assets.isLoaded(activeAudioFile)) {
						assets.unload(activeAudioFile);
					}
				}
				assets.load(newActiveAudioFile, Music.class);
				assets.finishLoadingAsset(newActiveAudioFile);
				challengeAudio = assets.get(newActiveAudioFile, Music.class);
				challengeAudio.play();
				activeAudioFile = newActiveAudioFile;
			}
		});
	}

	private Random coinTosser = new Random();

	private boolean isHeads() {
		return coinTosser.nextBoolean();
	}

	protected ICard<CardData> getNextCard() {
		log("getNextCard");
		/**
		 * Time marches on... for all cards in play
		 */
		activeDeck.updateTimeBy((long) (currentElapsed * 1000f));
		discardsDeck.updateTimeBy((long) (currentElapsed * 1000f));

		if (activeDeck.hasCards()) {
			log("Have " + activeDeck.size() + " cards left... ");
			// always re-shuffle deck each play
			activeDeck.shuffleThenSortByShowAgainDelay();

			ICard<CardData> card = activeDeck.topCard();
			CardStats cs = card.getCardStats();
			// check to try and avoid repeating same card sequentially
			if (card.id().equals(previousCardId)) {
				log("Maybe not repeating same card twice in a row...");
				// infinite loop prevention
				previousCardId = "" + System.currentTimeMillis();
				cs.setShowAgainDelay_ms(CardUtils.getNextInterval(cs.getPimsleurSlot()));
				cs.pimsleurSlotInc();
				discardsDeck.add(card);
				return getNextCard();
			}
			previousCardId = card.id();
			// go ahead and assign it to the discards deck
			discardsDeck.add(card);
			// reduce tries left count
			log("For card: " + card.getData().text);
			log("- Tries remaining: " + cs.getTriesRemaining());
			cs.triesRemainingDec();
			// update next show time delay based on current Pimsleur slot #
			cs.setShowAgainDelay_ms(CardUtils.getNextInterval(cs.getPimsleurSlot()));
			// move to next Pimsleur slot
			cs.pimsleurSlotInc();
			// inc count of how many times card has been displayed.
			cs.setShown(cs.getShown() + 1);
			// return this now "active" card for challenging with
			return card;
		}

		completedCardsCheck();
		discardsReadyForShowCheck();

		// ok, if we have active cards, do getNextCard
		log("Now have " + activeDeck.size() + " cards in play ...");
		if (activeDeck.hasCards()) {
			return getNextCard();
		}

		addCardFromMasterDeck();
		// ok, if we have active cards, do getNextCard
		log("Now have " + activeDeck.size() + " cards in play ...");
		if (activeDeck.hasCards()) {
			return getNextCard();
		}

		addCardsFromCompletedDeck();
		// ok, if we have active cards, do getNextCard
		log("Now have " + activeDeck.size() + " cards in play ...");
		if (activeDeck.hasCards()) {
			return getNextCard();
		}

		addAllFromDiscards();

		// ok, if we have active cards, do getNextCard
		log("Now have " + activeDeck.size() + " cards in play ...");
		if (activeDeck.hasCards()) {
			return getNextCard();
		}

		// No cards found in any deck! Bad things are happening!
		return null;
	}

	private void addAllFromDiscards() {
		// put all discards into the active deck w/o looking at show next times
		log("Moving ALL discards into active deck ...");
		discardsDeck.shuffleThenSortByShowAgainDelay();
		while (discardsDeck.hasCards()) {
			activeDeck.add(discardsDeck.topCard());
		}
	}

	private void addCardsFromCompletedDeck() {
		/**
		 * We still don't have any active cards, grab some from the completed
		 * deck. Being sure to set all stats to "never shown/correct". This is
		 * like an emergency don't crash me measure.
		 */
		log("Retrieving old cards from completed deck...");
		completedDeck.shuffleThenSortByNextSession();
		for (int count=0; count<3; count++) {
			if (completedDeck.size() != 0) {
				ICard<CardData> topCard = completedDeck.topCard();
				topCard.resetStats();
				topCard.resetTriesRemaining();
				topCard.getCardStats().setPimsleurSlot(0);
				activeDeck.add(topCard);
			}
		}
	}

	private void addCardFromMasterDeck() {
		/**
		 * We don't have any active cards, add one from the master deck. Being
		 * sure to set all stats to "never shown/correct".
		 */
		log("Getting new card from master deck ...");
		masterDeck.shuffleThenSortIntoGroups(3);
		if (masterDeck.size() != 0) {
			ICard<CardData> topCard = masterDeck.topCard();
			topCard.resetStats();
			topCard.resetTriesRemaining();
			topCard.getCardStats().setPimsleurSlot(0);
			activeDeck.add(topCard);
		}
	}

	private void discardsReadyForShowCheck() {
		/**
		 * If top card is ready for display, go ahead and recycle all of the
		 * discards.
		 */
		log("Scanning discards for cards to put back into immediate play... ");
		discardsDeck.shuffleThenSortByShowAgainDelay();
		log("Next show time for discards is: " + discardsDeck.getNextShowTime());
		if (discardsDeck.getNextShowTime() < 15000l) {
			while (discardsDeck.hasCards()) {
				activeDeck.add(discardsDeck.topCard());
			}
		}
	}

	/**
	 * Move to "completed" any cards that don't have tries left taking care to
	 * update Leitner boxes as needed.
	 */
	private void completedCardsCheck() {
		log("Scanning discards for newly completed cards... ");
		ListIterator<ICard<CardData>> iDiscards = discardsDeck.cardsIterator();
		while (iDiscards.hasNext()) {
			ICard<CardData> card = iDiscards.next();
			CardStats cardStats = card.getCardStats();
			if (cardStats.getTriesRemaining() > 0) {
				continue;
			}
			log("=== Completed card: " + card.id());
			// if correct, the card should get moved to the next
			// leitner box
			if (cardStats.isCorrect()) {
				cardStats.leitnerBoxInc();
				cardStats.setNextSessionShow(CardUtils.getNextSessionIntervalDays(cardStats.getLeitnerBox()));
				iDiscards.remove(); // remove then add
				completedDeck.add(card);
				log("- Moved to Leitner box: " + card.id() + " [" + cardStats.getLeitnerBox() + "]");
				log("- Days to show again: " + cardStats.getNextSessionShow());
				continue;
			}
			// if wrong, it should get moved to the previous
			// leitner box
			cardStats.leitnerBoxDec();
			cardStats.setNextSessionShow(CardUtils.getNextSessionIntervalDays(cardStats.getLeitnerBox()));
			iDiscards.remove(); // remove then add
			completedDeck.add(card);
			log("- Moved to Leitner box: " + card.id() + " [" + cardStats.getLeitnerBox() + "]");
			log("- Days to show again: " + cardStats.getNextSessionShow());
		}
	}

	@Override
	public void render(float delta) {
		super.render(delta);
	}

	private ClickListener onBack = new ClickListener() {
		@Override
		public void clicked(InputEvent event, float x, float y) {
			onBack();
		};
	};

	@Override
	protected boolean onBack() {
		userPause();
		pausedStage.getRoot().clearChildren();
		Dialog cancelSession = new Dialog("CANCEL SESSION?", skin) {
			@Override
			protected void result(Object object) {
				if ("YES".equals(object)) {
					game.previousScreen();
				}
				userResume();
			}
		};
		cancelSession.setModal(true);
		cancelSession.getTitleLabel().setAlignment(Align.center);
		cancelSession.button("ᎥᎥ - YES", "YES");
		cancelSession.button("ᎥᏝ - NO", "NO");
		cancelSession.getContentTable().row();
		cancelSession.text("Are you sure you want to cancel?");
		cancelSession.getContentTable().row();
		cancelSession.text("You will lose all of your");
		cancelSession.getContentTable().row();
		cancelSession.text("progress if you choose ᎥᎥ!");
		cancelSession.show(pausedStage);
		return true;
	}

	private Action actionFirstTime() {
		return Actions.run(new Runnable() {
			@Override
			public void run() {
				firstTime();
			}
		});
	}

	protected void firstTime() {
		log("SHOWING FIRST TIME DIALOG");
		userPause();
		pausedStage.getRoot().clearChildren();
		Dialog firstTimeNotice = new Dialog("HOW THIS WORKS", skin) {
			@Override
			protected void result(Object object) {
				userResume();
				stage.addAction(actionLoadNextChallenge());
			}
		};
		firstTimeNotice.setModal(true);
		firstTimeNotice.setFillParent(true);
		firstTimeNotice.getTitleLabel().setAlignment(Align.center);
		firstTimeNotice.button("ᎰᏩ");

		firstTimeNotice.getContentTable().row();
		Table noticeTable = new Table(skin);
		noticeTable.defaults().expand().fill();
		firstTimeNotice.getContentTable().add(noticeTable).expand().fill();
		String txt = Gdx.files.internal("text/how-this-works.txt").readString(StandardCharsets.UTF_8.name());
		Label message = new Label(txt, skin);
		message.setFontScale(.85f);
		message.setWrap(true);
		noticeTable.add(message);
		firstTimeNotice.show(pausedStage);
	}

	@Override
	protected boolean onMenu() {
		return false;
	}

	protected void discardImageFor(String imageFile) {
		if (assets.isLoaded(IMAGES_BACKDROP)) {
			assets.unload(IMAGES_BACKDROP);
		}
		if (assets.isLoaded(IMAGES_OVERLAY)) {
			assets.unload(IMAGES_OVERLAY);
		}
		if (assets.isLoaded(imageFile)) {
			assets.unload(imageFile);
		}
	}

	protected Image[] getImageFor(String imageFile) {
		if (!assets.isLoaded(IMAGES_BACKDROP)) {
			assets.load(IMAGES_BACKDROP, Texture.class);
			assets.finishLoadingAsset(IMAGES_BACKDROP);
		}
		if (!assets.isLoaded(IMAGES_OVERLAY)) {
			assets.load(IMAGES_OVERLAY, Texture.class);
			assets.finishLoadingAsset(IMAGES_OVERLAY);
		}
		if (!assets.isLoaded(imageFile)) {
			assets.load(imageFile, Texture.class);
			assets.finishLoadingAsset(imageFile);
		}
		assets.finishLoading();
		Image backdrop = new Image(assets.get(IMAGES_BACKDROP, Texture.class));
		Image image = new Image(assets.get(imageFile, Texture.class));
		Image overlay = new Image(assets.get(IMAGES_OVERLAY, Texture.class));
		backdrop.setScaling(Scaling.fit);
		image.setScaling(Scaling.fit);
		overlay.setScaling(Scaling.fit);
		Stack choice1 = new Stack();
		choice1.addActor(backdrop);
		choice1.addActor(image);
		choice1.addActor(overlay);
		return new Image[] { backdrop, image, overlay };// choice1;
	}

	private void prepDecks() {
		// clamp leitner box values
		for (ICard<CardData> card : activeDeck.getCards()) {
			CardStats cardStats = card.getCardStats();
			cardStats.setLeitnerBox(Math.max(cardStats.getLeitnerBox(), 0));
		}
		// reset basic statistics and scoring and tries remaining
		for (ICard<CardData> card : activeDeck.getCards()) {
			CardStats cardStats = card.getCardStats();
			card.resetStats();
			card.resetTriesRemaining();
			card.getCardStats().setPimsleurSlot(0);
		}
		// dec next session counter for active cards
		for (ICard<CardData> card : activeDeck.getCards()) {
			CardStats cardStats = card.getCardStats();
			cardStats.setNextSessionShow(Math.max(cardStats.getNextSessionShow() - 1, 0));
		}
		// go ahead and move to the completed deck any cards in the active
		// deck that are scheduled for later sessions
		ListIterator<ICard<CardData>> li = activeDeck.cardsIterator();
		while (li.hasNext()) {
			ICard<CardData> card = li.next();
			CardStats cardStats = card.getCardStats();
			if (cardStats.getNextSessionShow() > 0) {
				li.remove(); // remove before add!
				completedDeck.add(card);
				continue;
			}
		}
		// shuffle the remaining active cards
		activeDeck.shuffleThenSortByShowAgainDelay();
	}

	private ClickListener playAudioChallenge = new ClickListener() {
		public void clicked(InputEvent event, float x, float y) {
			if (challengeAudio != null && !challengeAudio.isPlaying()) {
				challengeAudio.play();
			}
		}
	};

	protected TextButton btnReplay;
	protected final Label lblCountdown;

	private void initUi() {
		uiTable = new Table(skin);
		uiTable.setTouchable(Touchable.childrenOnly);
		uiTable.defaults().expandX().top();
		TextButton btnBack = new TextButton(CLL2EV1.BACKTEXT, skin);
		btnBack.getLabel().setFontScale(.7f);
		btnReplay = new TextButton("[AUDIO]", skin);
		btnReplay.getLabel().setFontScale(.7f);
		btnReplay.addListener(playAudioChallenge);
		btnBack.pack();
		btnBack.addListener(onBack);
		uiTable.row();
		uiTable.add(btnBack).left();
		uiTable.add(lblCountdown).center();
		uiTable.add(btnReplay).right();

		gameTable = new Table(skin);
		gameTable.setFillParent(true);
		gameTable.defaults().expand().fill();
		gameTable.setTouchable(Touchable.childrenOnly);

		challengeTable = new Table(skin);
		challengeTable.row();
		challengeText.setText("...");
		challengeTable.add(challengeText).expandX();

		answersTable = new Table(skin);
		answersTable.defaults().expand().fill().pad(4);

		answersTable.row();
		answersTable.add(choice1);
		answersTable.add(choice2);

		gameTable.row();
		gameTable.add(challengeTable).expand(true, false).fillX();
		gameTable.row();
		gameTable.add(answersTable);
		gameTable.row();
		gameTable.add(uiTable).expand(true, false).fillX();

		stage.addActor(gameTable);
	}

	private ClickListener maybe1 = new ClickListener() {
		public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
			if (correct == 1) {
				choice1.addActor(imgCheckmark);
				ding();
			} else {
				choice1.addActor(imgXmark);
				if (activeCardStats.isCorrect()) {
					activeCardStats.pimsleurSlotDec();
					activeCardStats.triesRemainingInc();
					activeCardStats.setCorrect(false);
				}
				buzz();
			}
			choice1.setTouchable(Touchable.disabled);
			choice2.setTouchable(Touchable.disabled);
			// update card with total time it has been on display
			activeCardStats.setTotalShownTime(activeCardStats.getTotalShownTime() + currentElapsed);
			stage.addAction(actionLoadNextChallenge());
			return true;
		};
	};
	private ClickListener maybe2 = new ClickListener() {
		public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
			if (correct == 2) {
				choice2.addActor(imgCheckmark);
				ding();
			} else {
				choice2.addActor(imgXmark);
				if (activeCardStats.isCorrect()) {
					activeCardStats.pimsleurSlotDec();
					activeCardStats.triesRemainingInc();
					activeCardStats.setCorrect(false);
				}
				buzz();
			}
			choice1.setTouchable(Touchable.disabled);
			choice2.setTouchable(Touchable.disabled);
			// update card with total time it has been on display
			activeCardStats.setTotalShownTime(activeCardStats.getTotalShownTime() + currentElapsed);
			stage.addAction(actionLoadNextChallenge());
			return true;
		};
	};

	private Sound ding;

	private void ding() {
		if (ding == null) {
			if (!assets.isLoaded(DING)) {
				assets.load(DING, Sound.class);
				assets.finishLoadingAsset(DING);
			}
			ding = assets.get(DING, Sound.class);
		}
		ding.play(.4f);
	}

	private Sound buzz;

	private void buzz() {
		if (buzz == null) {
			if (!assets.isLoaded(BUZZER)) {
				assets.load(BUZZER, Sound.class);
				assets.finishLoadingAsset(BUZZER);
			}
			buzz = assets.get(BUZZER, Sound.class);
		}
		buzz.play(.6f);
	}

}