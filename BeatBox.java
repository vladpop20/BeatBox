import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.io.*;
import java.util.*;
import java.awt.event.*;

public class BeatBox {

	JPanel mainPanel;
	ArrayList<JCheckBox> checkboxList;                        // We store the checkboxes in an ArrayList
	Sequencer sequencer;
	Sequence sequence;
	Track track;
	JFrame theFrame;

	// These are the names of the instruments, for GUI labels (on each row)
	String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
			"High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
			"High Agogo", "Open Hi Conga"};

	// These represent the actual drum "keys"
	int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};


	public static void main(String[] args) {
		new BeatBox().buildGui();
	}


	public void buildGui() {
		theFrame = new JFrame("Cyber BeatBox");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);                // Assign the new Layout Manager to the Panel
		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Creates a margin between the edges of the panel and rest of components are placed. Like SetPadding in JavaFX

		checkboxList = new ArrayList<>();
		Box buttonBox = new Box(BoxLayout.Y_AXIS);            // Creates the box that will hold the control buttons
		buttonBox.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));;

		JButton start = new JButton("Start");
		start.addActionListener(new MyStartListener());        // MyStartListener is the inner class that implements ActionListener interface
		buttonBox.add(start);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 5)));  // Dimension(width: 0, height: 5);   I created this rigid component to set a border for next button to arrive

		JButton stop = new JButton("Stop");
		stop.addActionListener(new MyStopListener());        // Same as start button
		buttonBox.add(stop);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 5)));

		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener(new MyUpTempoListener()); // Same as start button	
		buttonBox.add(upTempo);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 5)));

		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(new MyDownTempoListener());  // Same as start button	
		buttonBox.add(downTempo);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 5)));

		JButton save = new JButton("Save");			// Saves the current pattern of checkboxes
		save.addActionListener(new MySendListener());
		buttonBox.add(save);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 5)));

		JButton load = new JButton("Load");			// Load the pattern of one of the model of chosen checkboxes
		load.addActionListener(new MyReadInListener());
		buttonBox.add(load);
		buttonBox.add(Box.createRigidArea(new Dimension(0, 5)));

		Box nameBox = new Box(BoxLayout.Y_AXIS);            // Creates the box that will hold all the instruments names
		for (int i = 0; i < 16; i++) {                        // This populates the nameBox with new instrument labels
			nameBox.add(new Label(instrumentNames[i]));
		}

		background.add(BorderLayout.EAST, buttonBox);        // Assing the each Box, to each side of the background panel
		background.add(BorderLayout.WEST, nameBox);

		theFrame.getContentPane().add(background);            // By default, frame's BorderLayout manager will set the background to the CENTER

		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(1);
		grid.setHgap(2);
		mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);        // Add the mainPanel(that contains the grid) to the background Panel

		for (int i = 0; i < 256; i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkboxList.add(c);
			mainPanel.add(c);                                // Makes the checkBoxes(false, so they aren't checked), and add them to the List, then adds to the FlowLayout of mainPanel
		}

		setUpMidi();
		// setBounds((int x, int y, int width, int height)
		theFrame.setBounds(50, 50, 300, 300);                // Moves and resizes this component. The new location of the top-left corner is specified by x and y, and the new size is specified by width and height.
		theFrame.pack();                                    // Causes this Window/Frame to be sized to fit the preferred size and layouts of its subcomponents.
		theFrame.setVisible(true);
	}


	public void setUpMidi() {                                // The usual MIDI set-up for getting the Sequencer, Sequence, and Track
		try {
			sequencer = MidiSystem.getSequencer();            // Creates a Sequencer
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);        // The tempo-based timing type(PPQ), for which the resolution(4) is expressed in pulses (ticks) per quarter note.
			track = sequence.createTrack();                    // The MIDI data, lives in track


		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	// This is where we turn checkBoxes state into MIDI events, and add them to the Track
	public void buildTrackAndStart() {
		int[] trackList = null;                                // We'll make a 16-element array to hold the values for one instrument, across all 16 beats.

		sequence.deleteTrack(track);                        // get rid of the old Track, and create a new one.
		track = sequence.createTrack();

		for (int i = 0; i < 16; i++) {                        // do this for each of the 16 rows (for example: Bass, Hand Clap, etc.)
			trackList = new int[16];

			int key = instruments[i];                        // Set the "key". that represent which instrument this is (Bass, Hi-Hat, etc.)

			for (int j = 0; j < 16; j++) {                    // Do this for each of the BEATS for this row

				JCheckBox jc = checkboxList.get(j + (16 * i));        // This "i" represent the actual row, from 1 to 16, because we have 256 checkBoxes, 16 per row

				if (jc.isSelected()) {                        // If the checkBox at this beat is selected, put the key value in this lot in the array
					trackList[j] = key;                        // Otherwise, the instrument is NOT supposed to play at this beat, so set to ZERO.
				} else {
					trackList[j] = 0;
				}
			}

			makeTracks(trackList);                            // For this instrument and for all his 16 beats.

			track.add(makeEvent(176, 1, 127, 0, 16));        // Here's how we pick the beat, we instert our on ControllerEvent (176 says the event type is ControllerEvent), with the argument #127.
			// This event will do nothing. I put it in just so we can get an event each time a note is played. (We can't listen otherwise for instrument note)
		}

		track.add(makeEvent(192, 9, 1, 0, 15));                // We always want to make sure that there IS an event at beat 16 (0 to 15).
		// Otherwise, the BeatBox might not go to the full 16 beats, before it starts over.
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);   // We use this to play in continuous looping
			sequencer.start();
			sequencer.setTempoInBPM(120);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public class MyStartListener implements ActionListener {        // First of the inner classes, listeners for the buttons.
		public void actionPerformed(ActionEvent a) {
			buildTrackAndStart();
		}
	}

	public class MyStopListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			sequencer.stop();
		}
	}

	public class MyUpTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * 1.03));
		}
	}

	public class MyDownTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * 0.97));
		}
	}


	public void makeTracks(int[] list) {                    // This makes events for one instrument at a time, for all 16 beats.
		for (int i = 0; i < 16; i++) {
			int key = list[i];

			if (key != 0) {
				track.add(makeEvent(144, 9, key, 100, i));            // Make the NOTE ON and NOTE OFF events, and add them to the Track.
				track.add(makeEvent(128, 9, key, 100, i + 1));
			}
		}
	}

	public MidiEvent makeEvent(int command, int channel, int note, int velocity, int tick) {
		MidiEvent event = null;
		try {
			ShortMessage a = new ShortMessage();
			a.setMessage(command, channel, note, velocity);
			event = new MidiEvent(a, tick);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return event;
	}

	public class MySendListener implements ActionListener {    // This inner class exists for saving the current pattern
		public void actionPerformed(ActionEvent event) {

			boolean[] checkboxState = new boolean[256];        // This boolean array holds the state of each checkBox 

			for (int i = 0; i < 256; i++) {
				JCheckBox check = checkboxList.get(i);        // Walk through the checkboxList (ArrayList of checkboxes), and get the state of each one
				if (check.isSelected()) {                    // and add it to the boolean array
					checkboxState[i] = true;
				}
			}

																	// This part, just deals with writing / serializing the boolean array   						(( !! ))
			try {
				JFileChooser fileSave = new JFileChooser();        // Brings up a file dialog box and waits on this line until the user chooses 'Save' from the dialog box.
				fileSave.showSaveDialog(theFrame);
				File newBornFile = fileSave.getSelectedFile();

				FileOutputStream fileStream = new FileOutputStream(newBornFile);
				ObjectOutputStream os = new ObjectOutputStream(fileStream);
				os.writeObject(checkboxState);										// (( !! )) BOOLEAN PRIMITIVE IS SERIALIZABLE  (( !! ))

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public class MyReadInListener implements ActionListener {    // This inner class "restore" loads the saved pattern back in, and resets the checkboxes.
		public void actionPerformed(ActionEvent event) {

			boolean[] checkboxState = null;

			try {
				JFileChooser fileOpen = new JFileChooser();        // File Object the user chose from the open file dialog.
				fileOpen.showOpenDialog(theFrame);
				File loadedFile = fileOpen.getSelectedFile();

				FileInputStream fileIn = new FileInputStream(loadedFile);
				ObjectInputStream is = new ObjectInputStream(fileIn);
				checkboxState = (boolean[]) is.readObject();    // Read the single object in the file (the boolean array) and cast it back to a boolean array
				// readObject return a reference of type Object.
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			for (int i = 0; i < 256; i++) {
				JCheckBox check = checkboxList.get(i);
				if (checkboxState[i]) {                            // Restore the state of each of the checkboxes in the ArrayList,
					// of actual JCheckBox objects (checkboxList)
					check.setSelected(true);
				} else {
					check.setSelected(false);
				}
			}

			sequencer.stop();                                    // Now stop whatever is currently playing, and rebuild the sequence
			buildTrackAndStart();                                // using he new state of the checkboxes in the ArrayList
		}
	}
}
			
			
			
			
		
		
		
		
		
		