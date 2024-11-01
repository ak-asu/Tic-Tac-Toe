# Tic-Tac-Toe Game
Tic Tac Toe Multiplayer Game with Bluetooth Connectivity.

## Project Description
This project is a fully-featured Tic-Tac-Toe game with multiple game modes such as play against computer, play against human on single and separate devices.

The application leverages Bluetooth to handle device discovery (along with Location), pairing, connecting and data transfer. Built-in error handling manages connection disruptions and permissions are requested interactively as needed. If a Bluetooth connection is lost, the application accordingly exits the game screen and releases resources if required. This ensures a robust and smooth user experience.

Using Jetpack Compose, the application follows a modern and declarative UI approach. Key features include a Bluetooth device list, connection status displays, settings menu, home, career, score and game related views. Everything is designed as modular composables to enhance code reusability and maintenance. Navigation is powered by the Navigation component which facilitates smooth screen transitions, avoids redundant back-stack entries and supports intuitive user flow. The game screen is locked in portrait orientation for optimal gameplay while other screens maintain flexible orientations to support usability.

## Functional Description
1. **Player vs Computer mode**: Users can challenge the AI and adjust the difficulty level during gameplay. This mode utilizes Minimax with Alpha-Beta pruning for efficient and dynamic moves based on the selected difficulty.
2. **Player vs Player on one device mode**: Two players can compete on the same device with each taking turns.
3. **Player vs Player on separate devices mode**: Bluetooth should be enabled and permissions should be allowed to play this mode. With an interactive interface, the application automatically requests for permissions or starts Bluetooth if needed. On clicking the "Play on 2-Devices" button, a dialog box displaying available and paired devices is shown. On selecting a device and the preference, the user waits for acceptance from the second device to proceed with the gameplay. The connected devices transmit data in JSON format and connection interrupts or disabling services is handled accordingly without crashing the application.
4. **Home Screen**: The home screen provides options for different game modes, settings and a button to view game history.
5. **Game Screen**: The game screen shows a 3x3 grid where players mark their moves. The turn and marker are indicated by the highlighted text above the grid. If a player wins, an animated line highlights the winning row, column or diagonal. A "Reset" button allows players to restart the game with current configurations. The game also detects when a draw is inevitable.
6. **Score Screen**: At the end of each game, this screen shows the final result. It also provides a "Replay" button to restart the game.
7. **Career Screen**: Displays historical game records with win/loss statistics. The screen can be viewed by clicking on the "View Career" button.
8. **Settings Menu**: Players can adjust game difficulty, change theme and control volume. They can also set preferences for turn-taking when playing against computer or anther human on single device.
9. **User Feedback and Notifications**: Sound effects indicate game events like wins, losses, and draws. Toast messages inform users of status changes including Bluetooth connection states and error handling.


## Usage Instructions
1. **Installation**: Clone the GitHub repository and open the project in Android Studio.
2. **Run**: Connect your Android device to the computer and select Run in Android Studio to build and launch the app on your device.
3. **Player vs Computer**: Click on "Play against Computer" to play against a computer with the desired difficulty chosen in Settings menu. Select turn preference between "First", "Second" and "No Preference" if prompted. "No Preference" will randomly assign a preference to the user.
4. **Player vs Player on one device**: Click on "Play on 1-Device" to play on the same device. Select turn preference if prompted.
4. **Player vs Player on two devices**: Grant necessary Bluetooth permissions and services when prompted. To initiate multiplayer, click "Play on 2-Devices" button. Choose a device from the available devices or paired devices list. The application will automatically initiate a pairing request if devices are not paired. When connected, both devices will be prompted to select between "Me" and "Opponent" to specify the turn preference. Once a preference is selected by a player, the application waits for the other player to hit "Play" to start the game.


## Contributors
Developed by CSE 535 Project Group 25, with team members collaborating on module development, integration, and testing to ensure a comprehensive, polished user experience.