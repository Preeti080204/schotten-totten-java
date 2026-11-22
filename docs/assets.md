# Assets and their origin

Every image, font and sound is bundled in `src/main/resources/com/schottentotten/view/`, so the game runs offline. **No illustration, logo or card face from the IELLO rulebook or the commercial game is used.** The rulebook served only as a style reference: a playful medieval Scottish look, painted landscapes, clan colours, parchment and wood.

## Images (`images/`)

Every image is an original illustration made for this project with an AI image generator, prompted from written descriptions (no rulebook artwork was given as input). The sources are kept outside the repository. Text and numbers on the cards are drawn by the program (`CardView`), not painted into the images.

| File | Content | Size |
|---|---|---|
| `menu-landscape.jpg` | Highlands valley with the border Stones (main menu, blurred behind other screens) | 1280×720 |
| `board-meadow.jpg` | Flowery meadow seen from above (game board) | 1280×720 |
| `parchment.jpg` | Seamless parchment texture (panels, dialogs) | 768×768 |
| `wood.jpg` | Dark wood texture (side rail) | 512×512 |
| `stone-tile.png` | Carved border Stone with a Celtic knot; background removed with `scripts/tools/CutoutBackground.java` | 400×400 |
| `cards/clan-<colour>.jpg` (6) | One clan warrior per colour: red, orange, yellow, green, blue, purple | 540×720 |
| `cards/tactic-*.jpg` (9) | One illustration per Tactic card (the two Jokers share one) | 540×720 |
| `cards/expansion-*.jpg` (3) | Highland Scout, Stone Ward, Clan Exchange | 540×720 |
| `cards/card-back.jpg` | Card back with a Celtic knot | 540×720 |

## Fonts (`fonts/`)

All three families are under the SIL Open Font License 1.1, which allows bundling them in software. The licence texts are included next to the font files.

| Font | Use | Licence file |
|---|---|---|
| Uncial Antiqua (Astigmatic) | Game title and ribbons | `OFL-UncialAntiqua.txt` |
| Cinzel Decorative (Natanael Gama) | Headings, panel titles, player names and card numbers | `OFL-CinzelDecorative.txt` |
| Alegreya Sans (Huerta Tipográfica) | Body text and card names | `OFL-AlegreyaSans.txt` |

## Sounds (`sounds/`)

All audio is synthesised by `scripts/tools/SoundSynth.java` (plucked-string harp by Karplus-Strong synthesis, drones and filtered noise). No recording or sample library is used. The ten effects are WAV files. The ambient music loop was converted to AAC with `afconvert -d aac@44100`.

To regenerate them: `java scripts/tools/SoundSynth.java <output-directory>`.

## Interface graphics

Ribbons, plaques, Stone glows, ownership banners, confetti and card frames are drawn with JavaFX shapes and CSS (`view/style.css`, `Styles`, `Ribbon`, `StoneView`, `CardView`).
