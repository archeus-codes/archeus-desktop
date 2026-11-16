# archeus-desktop

A desktop editor for Nintendo DS cheat databases, on macOS, Windows and Linux.

![The main window: a tree of games with their icons and folders beside the selected game's title, Game ID, master
code and box art](docs/images/main-window.png)

## Origin

This app was extracted from the Archeus monorepo, in development since February 2021. Archeus is a curated Nintendo
DS cheat database with a website and this desktop editor. Work on the editor itself began in April 2026. The app reads
and writes files with [archeus-core](https://github.com/archeus-codes/archeus-core).

## Features

### Files

| Format | Files | Open | Save |
|---|---|---|---|
| R4 binary (R4CCE, most flashcarts and kernels) | `usrcheat.dat` | Yes | Yes |
| R4CCE XML | `cheats.xml` | Yes | Yes |
| Action Replay DS(i) XML | `cheats.xml` | Yes | Yes |
| EDGE and iEDGE | `cheats.dat` | Yes | Yes |
| CycloDS Evolution | `user.evoCheats` | Yes | Yes |
| M3 DS Real / M3i Zero | `cheat_EN.db` | Yes | Yes |
| EZ-Flash V (original menu) | `ezarcode.dat` | Yes | Yes |

EDGE and CycloDS have limits: 4,096 games in a file, 255 codes in a folder and 255 lines in a code. Saving past them
splits a large folder into several, writes a large database as one file per range of letters and leaves out codes
that are too long, after a warning that lists every change.

An M3 file has no master codes, one at a time folders or codes switched on, and leaves out codes with placeholder
lines. Its text is GBK, so characters GBK lacks become `?`. Saving as one warns about each first.

An EZ-Flash `ezarcode.dat` has no master codes, so saving as one warns about them first.

### Editing

- A tree of games, folders and codes, reordered by dragging, with cut, copy and paste as R4CCE text.
- A pane for the selected item: titles, comments, code lines, the Game ID and the master code, each checked as you
  type.
- A warning when a code you've changed has structural problems, such as a condition with no terminator, using the
  same check as the site. You can keep editing or keep it anyway.
- Load ROM, which reads a game's ID from its ROM, and Find Duplicates, which finds games sharing an ID.
- A Button Activator that wraps a code so it runs only while chosen buttons are held.
- Export of one game for melonDS (`.mch`), RetroArch (`.cht`) or DeSmuME (`.dct`).

### Look

- Box art and game icons, downloaded from [archeus.codes](https://archeus.codes) under Tools.
- Light and dark themes (AtlantaFX's Nord, Primer, Dracula and Cupertino), or one that follows the system.

## Usage

Start by downloading the box art and game icons with Tools → Download Box Art and Icons. Until then, every game in
the tree has the same default icon and its pane shows a plain box. The first download takes a few minutes, and
later ones fetch only the images that are new or changed.

Open a file with File → Open, or drop it on the window. Select a game, folder or code in the tree to edit it in the
pane beside it. Save As converts the file to another format.

Copying a game, folder or code puts it on the clipboard as R4CCE text, which you can paste back into the tree or share
as text. Drag an item to move it: a game beside another game, a folder into a game or beside another folder, and a code
into a game or folder, or beside another code or folder.

### Shortcuts

| Action | macOS | Windows and Linux |
|---|---|---|
| Open | Cmd+O | Ctrl+O |
| Save | Cmd+S | Ctrl+S |
| Save As | Cmd+Shift+S | Ctrl+Shift+S |
| Add game | Cmd+N | Ctrl+N |
| Add folder to the selected game | Cmd+Shift+N | Ctrl+Shift+N |
| Add code to the selected game or folder | Cmd+D | Ctrl+D |
| Cut, copy, paste in the tree | Cmd+X, Cmd+C, Cmd+V | Ctrl+X, Ctrl+C, Ctrl+V |
| Delete the selected item | Cmd+Backspace | Delete |
| Search games by title | Cmd+F | Ctrl+F |
| Next search match | F3 or Cmd+G | F3 or Ctrl+G |
| Previous search match | Shift+F3 or Cmd+Shift+G | Shift+F3 or Ctrl+Shift+G |
| Jump to the tree's top or bottom | Cmd+Up, Cmd+Down, or Home, End | Ctrl+Up, Ctrl+Down, or Home, End |

### App data

Everything the app keeps is in `.archeus` in your home folder: `preferences.properties` (the theme and the last file
opened), `boxarts` and `icons` (downloaded under Tools), and, for the installed app, `logs`.
Deleting the folder resets the app.

## Downloads

Installers for macOS, Windows and Linux are published with each release on GitHub and on
[archeus.codes](https://archeus.codes).

The Linux installer is a `.deb` for Ubuntu 24.04 and later. On a system with no desktop menu, such as Ubuntu under WSL,
its install stops half configured, since it can't add the app to the menu. Create the menu folders, then finish it:

```
sudo mkdir -p /etc/xdg/menus /usr/share/applications /usr/share/desktop-directories
sudo dpkg --configure archeus
```

## Building

It needs JDK 25. The Maven Wrapper downloads Maven itself, so there's nothing else to install.

```
./mvnw javafx:run                              # run the app
./mvnw verify                                  # run the tests and check their coverage
./mvnw clean package -Pjpackage                # build the app in target/dist
./mvnw clean package -Pjpackage,installer      # also build its installer in target/installer
```

The tests start JavaFX, so they need a display; on Linux without one, run them under `xvfb-run`. The build fails when
the tests cover less than 85% of the lines. The installer is a `.dmg` on macOS, an `.msi` on Windows (which needs the
WiX Toolset 3) and a `.deb` on Linux (which needs `fakeroot`).

## Credits

- **[GameTDB](https://www.gametdb.com)**, for the box art the app downloads and the site shows.
- **[AtlantaFX](https://github.com/mkpaz/atlantafx)**, for the themes.
- **[Ikonli](https://github.com/kordamp/ikonli)** and the Material Design Icons, for the icons.
- The people credited in [archeus-core](https://github.com/archeus-codes/archeus-core#credits), whose work the file
  formats rest on.

## License

[GNU General Public License v3.0 or later](LICENSE). The libraries bundled in the app and its installers keep their
own licenses, collected in [THIRD-PARTY-LICENSES](THIRD-PARTY-LICENSES).

Copyright (c) 2026 Deiondre Andrews (Demonic722).
