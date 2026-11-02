package com.barding.ui;

import java.awt.event.KeyEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.game.chatbox.ChatboxInput;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.util.ImageUtil;

/**
 * The Barding level up box, drawn in the chatbox in the layout of the game's own. The game can't
 * show its box for a skill it doesn't have, so this is built from our own widgets, on this client only.
 */
@Singleton
public class LevelUpDialog
{
	/**
	 * A placeholder until the Air Guitar sound is identified: the sound the OSRS Wiki files as
	 * Level up fireworks. A plugin can play sound effects but has no way to play a jingle.
	 */
	private static final int SOUND = 2396;

	private static final int SPRITE_ICON = -25102; // negative ids can't clash with the game's
	private static final int ICON_HEIGHT = 40;
	private static final int ICON_X = 30;
	private static final int ICON_Y = 18;
	private static final int TEXT_X = 90;
	private static final int TEXT_RIGHT_PAD = 20;
	private static final int TITLE_Y = 10;
	private static final int BODY_Y = 34;
	private static final int CONTINUE_Y = 58;
	private static final int LINE_HEIGHT = 18;

	private static final int COLOR_TITLE = 0x000080;
	private static final int COLOR_BODY = 0x000000;
	private static final int COLOR_CONTINUE = 0x0000FF;
	private static final int COLOR_CONTINUE_HOVER = 0xFFFFFF;

	private final Client client;
	private final ChatboxPanelManager panels;
	private final InstrumentIcons icons;

	@Inject
	public LevelUpDialog(Client client, ChatboxPanelManager panels, InstrumentIcons icons)
	{
		this.client = client;
		this.panels = panels;
		this.icons = icons;
	}

	/** Shows the box and plays the sound. Client thread only. */
	public void show(int level)
	{
		client.getSpriteOverrides().put(SPRITE_ICON, ImageUtil.getImageSpritePixels(icons.skillIcon(ICON_HEIGHT), client));
		panels.openInput(new Panel(level));
		client.playSoundEffect(SOUND);
	}

	private final class Panel extends ChatboxInput implements KeyListener
	{
		private final int level;

		private Panel(int level)
		{
			this.level = level;
		}

		@Override
		protected void open()
		{
			Widget container = panels.getContainerWidget();
			int width = container.getWidth() - TEXT_X - TEXT_RIGHT_PAD;

			Widget icon = container.createChild(-1, WidgetType.GRAPHIC);
			icon.setSpriteId(SPRITE_ICON);
			place(icon, ICON_X, ICON_Y, icons.skillIconWidth(ICON_HEIGHT), ICON_HEIGHT);

			addText(container, "Congratulations, you've just advanced your Barding level.", COLOR_TITLE, TITLE_Y, width);
			addText(container, "Your Barding level is now " + level + ".", COLOR_BODY, BODY_Y, width);

			Widget proceed = addText(container, "Click here to continue", COLOR_CONTINUE, CONTINUE_Y, width);
			proceed.setHasListener(true);
			proceed.setAction(0, "Continue");
			proceed.setOnOpListener((JavaScriptCallback) ev -> panels.close());
			proceed.setOnMouseOverListener((JavaScriptCallback) ev -> proceed.setTextColor(COLOR_CONTINUE_HOVER));
			proceed.setOnMouseLeaveListener((JavaScriptCallback) ev -> proceed.setTextColor(COLOR_CONTINUE));
		}

		private Widget addText(Widget container, String value, int color, int y, int width)
		{
			Widget text = container.createChild(-1, WidgetType.TEXT);
			text.setText(value);
			text.setFontId(FontID.QUILL_8);
			text.setTextColor(color);
			text.setXTextAlignment(WidgetTextAlignment.CENTER);
			text.setYTextAlignment(WidgetTextAlignment.CENTER);
			place(text, TEXT_X, y, width, LINE_HEIGHT);
			return text;
		}

		private void place(Widget widget, int x, int y, int width, int height)
		{
			widget.setOriginalX(x);
			widget.setOriginalY(y);
			widget.setOriginalWidth(width);
			widget.setOriginalHeight(height);
			widget.revalidate();
		}

		@Override
		public void keyTyped(KeyEvent e)
		{
		}

		@Override
		public void keyPressed(KeyEvent e)
		{
			if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				e.consume();
				panels.close();
			}
		}

		@Override
		public void keyReleased(KeyEvent e)
		{
		}
	}
}
