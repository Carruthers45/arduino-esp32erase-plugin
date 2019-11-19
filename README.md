# Arduino ESP32 erase flash

Arduino plugin to ESP32 erase flash memory.

## Installation

- Make sure you use one of the supported versions of Arduino IDE and have ESP32 core installed.
- Download the tool archive from [releases page](https://github.com/tanakamasayuki/arduino-esp32erase-plugin/releases/latest).
- In your Arduino sketchbook directory, create tools directory if it doesn't exist yet.
- Unpack the tool into tools directory (the path will look like ```<home_dir>/Arduino/tools/ESP32Erase/tool/esp32erase.jar```).
- Restart Arduino IDE. 

On the OS X create the tools directory in ~/Documents/Arduino/ and unpack the files there

## Usage

- Open a sketch (or create a new one and save it).
- Make sure you have selected a board, port, and closed Serial Monitor.
- Select *Tools > ESP32 Erase Flash* menu item. This should start erasing ESP32 flash file system.

  When done, IDE status bar will display Erase Flash failed message.

## Credits and license

- Licensed under GPL v2 ([text](LICENSE.txt))
