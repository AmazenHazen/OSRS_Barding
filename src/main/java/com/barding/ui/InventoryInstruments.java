package com.barding.ui;

import com.barding.data.InstrumentDef;
import com.barding.data.InstrumentRepository;
import com.barding.progress.InstrumentInstance;
import com.barding.progress.InstrumentRank;
import com.barding.progress.InstrumentSkills;
import com.barding.progress.InstrumentStorage;
import com.barding.progress.NoteHit;
import com.barding.progress.PlayService;
import com.barding.progress.ProgressService;
import com.barding.progress.TuningService;
import com.barding.progress.TuningState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws carried instruments in empty inventory slots. They are pictures, not items: the game does
 * not know about them, so their right-click options are ours and only run inside the plugin.
 */
@Singleton
public class InventoryInstruments extends Overlay
{
	// The inventory is always a 4 by 7 grid of these
	private static final int COLUMNS = 4;
	private static final int SLOT_WIDTH = 36;
	private static final int SLOT_HEIGHT = 32;
	private static final int PITCH_X = 42;
	private static final int PITCH_Y = 36;

	private final Client client;
	private final InstrumentStorage storage;
	private final InstrumentSkills skills;
	private final ProgressService progress;
	private final TuningService tuningService;
	private final PlayService playService;
	private final InstrumentIcons icons;
	private final OpenMenus menus;
	private InstrumentRepository instruments;

	@Inject
	public InventoryInstruments(Client client, InstrumentStorage storage, InstrumentSkills skills, ProgressService progress,
		TuningService tuningService, PlayService playService, InstrumentIcons icons, OpenMenus menus)
	{
		this.playService = playService;
		this.client = client;
		this.storage = storage;
		this.skills = skills;
		this.progress = progress;
		this.tuningService = tuningService;
		this.icons = icons;
		this.menus = menus;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	public void setInstruments(InstrumentRepository instruments)
	{
		this.instruments = instruments;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Widget items = visibleInventory();
		if (items == null)
		{
			return null;
		}
		if (instruments == null)
		{
			return null;
		}
		for (InstrumentInstance instrument : storage.all())
		{
			if (instrument.isCarried())
			{
				instruments.find(instrument.getInstrumentId()).ifPresent(def -> draw(g, items, def, instrument));
			}
		}
		return null;
	}

	private void draw(Graphics2D g, Widget items, InstrumentDef def, InstrumentInstance instrument)
	{
		Rectangle slot = slotBounds(items, instrument.getSlot());
		BufferedImage icon = icons.get(def);
		g.drawImage(icon, slot.x + (slot.width - icon.getWidth()) / 2, slot.y + (slot.height - icon.getHeight()) / 2, null);
		if (tuningService.isTuning(def))
		{
			String percent = instrument.getTuning() + "%";
			g.setFont(FontManager.getRunescapeSmallFont());
			g.setColor(Color.BLACK);
			g.drawString(percent, slot.x + 1, slot.y + 11);
			g.setColor(InstrumentTab.tuningColor(instrument.getTuning()));
			g.drawString(percent, slot.x, slot.y + 10);
		}
	}

	/** Call once per frame while the right-click menu is being built. */
	public void addMenuOptions(Point mouse)
	{
		Widget items = visibleInventory();
		if (items == null || instruments == null)
		{
			return;
		}
		for (InstrumentInstance instrument : storage.all())
		{
			if (instrument.isCarried() && slotBounds(items, instrument.getSlot()).contains(mouse.getX(), mouse.getY()))
			{
				instruments.find(instrument.getInstrumentId()).ifPresent(def -> addOptions(def, instrument));
				return;
			}
		}
	}

	private void addOptions(InstrumentDef def, InstrumentInstance instrument)
	{
		String id = def.getId();
		menus.addOption("Examine", def.getName(), () ->
		{
			InstrumentRank rank = skills.getRank(def.getType());
			double chance = NoteHit.chance(progress.getLevel(), rank, instrument.getTuning(), NoteHit.STATIC_MASTERY);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				def.getName() + ": " + TuningState.of(instrument.getTuning()).getLabel() + " (" + instrument.getTuning() + "%). Your skill: "
					+ rank.getLabel() + ". Note hit chance: " + Math.round(chance) + "%.", null);
		});
		menus.addOption("Store", def.getName(), () ->
		{
			tuningService.stop();
			playService.stop();
			storage.store(id);
		});
		if (tuningService.canTune(instrument.getTuning()))
		{
			menus.addOption("Tune", def.getName(), () ->
			{
				playService.stop();
				tuningService.start(def);
			});
		}
		if (playService.isPlaying(def))
		{
			menus.addOption("Stop", def.getName(), playService::stop);
		}
		else
		{
			menus.addOption("Play", def.getName(), () ->
			{
				tuningService.stop();
				playService.stop();
				playService.start(def);
			});
		}
	}

	private Widget visibleInventory()
	{
		Widget items = client.getWidget(InterfaceID.Inventory.ITEMS);
		return items == null || items.isHidden() ? null : items;
	}

	/**
	 * Where the game puts a slot, as an offset from the inventory. Empty slots aren't drawn, so they have
	 * no screen position of their own. The grid is only a fallback, as it doesn't include the slots' margin.
	 */
	private static Rectangle slotBounds(Widget items, int slot)
	{
		Point origin = items.getCanvasLocation();
		Widget child = items.getChild(slot);
		int x = child != null ? child.getOriginalX() : slot % COLUMNS * PITCH_X;
		int y = child != null ? child.getOriginalY() : slot / COLUMNS * PITCH_Y;
		return new Rectangle(origin.getX() + x, origin.getY() + y, SLOT_WIDTH, SLOT_HEIGHT);
	}
}
