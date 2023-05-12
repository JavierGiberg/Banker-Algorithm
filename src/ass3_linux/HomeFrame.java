package ass3_linux;
/*
 * Javier Giberg
 */
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class HomeFrame {
	private static JTextArea runTextArea;
	private static JTextArea deadlockTextArea;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(HomeFrame::createAndShowGUI);
	}

	private static void createAndShowGUI() {

        JFrame frame = new JFrame("Process Simulation");
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        frame.getContentPane().add(mainPanel);

        runTextArea = new JTextArea();
        runTextArea.setEditable(false);

        JScrollPane runScrollPane = new JScrollPane(runTextArea);
        runScrollPane.setBorder(BorderFactory.createTitledBorder("Running Processes"));

        deadlockTextArea = new JTextArea();
        deadlockTextArea.setEditable(false);

        JScrollPane deadlockScrollPane = new JScrollPane(deadlockTextArea);
        deadlockScrollPane.setBorder(BorderFactory.createTitledBorder("Deadlocked Processes"));

        JPanel processPanel = new JPanel(new GridLayout(1, 2));
        processPanel.add(runScrollPane);
        processPanel.add(deadlockScrollPane);

        mainPanel.add(processPanel, BorderLayout.CENTER);

        frame.pack();
        frame.setSize(650, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Semaphore semaphore = new Semaphore(0, true);
        HashMap<Integer, String> runList = new HashMap<>();
        HashMap<Integer, String> deadlockList = new HashMap<>();

        /*
         * Install params with I/O
         */
        char character = 'A';

        int[] createAvailable = { 0, 0, 0, 0, 0 };
        for (int i = 0; i < createAvailable.length; i++) {
            String input = JOptionPane.showInputDialog(frame, "Enter Available [" + character + "]");
            createAvailable[i] = Integer.parseInt(input);
            character++;
        }
        Available available = new Available(createAvailable);

        String processCountInput = JOptionPane.showInputDialog(frame, "Enter the number of processes:");
        int processCount = Integer.parseInt(processCountInput);
        P[] processTable = new P[processCount];

        for (int i = 0; i < processTable.length; i++) {
            JOptionPane.showMessageDialog(frame, "Handle [P" + (i + 1) + "] Matrices");

            character = 'A';
            int[] allocation = { 0, 0, 0, 0, 0 };
            int[] max = { 0, 0, 0, 0, 0 };
            for (int j = 0; j < allocation.length; j++) {
                String allocationInput = JOptionPane.showInputDialog(frame, "Enter Allocation [" + character + "]:");
                allocation[j] = Integer.parseInt(allocationInput);

                String maxInput = JOptionPane.showInputDialog(frame, "Enter Max [" + character + "]:");
                max[j] = Integer.parseInt(maxInput);

                character++;
            }
            processTable[i] = new P(available, max, allocation, semaphore, runList, deadlockList);
        }

        semaphore.release(1);
        for (P process : processTable)
            new Thread(process).start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            StringBuilder runText = new StringBuilder("Run Process <");
            for (String process : runList.values()) {
                runText.append(" ").append(process).append(" ,");
            }
            runText.append(">");
            runTextArea.setText(runText.toString());

            StringBuilder deadlockText = new StringBuilder("Deadlock Process <");
            for (String process : deadlockList.values()) {
                deadlockText.append(" ").append(process).append(" ,");
            }
            deadlockText.append(">");
            deadlockTextArea.setText(deadlockText.toString());
        });
    }


	static class P implements Runnable {
		private static int count = 1;
		private static int countLoop = 0;
		private static int raceCondition = 0;
		private int id;
		private int[] need;
		private Available available;
		private int[] max;
		private int[] allocation;
		Semaphore semaphore;
		HashMap<Integer, String> runList;
		HashMap<Integer, String> deadlockList;

		public P(Available available, int[] max, int[] allocation, Semaphore semaphore,
				HashMap<Integer, String> runList, HashMap<Integer, String> deadlockList) {
			this.id = count;
			count++;
			this.allocation = allocation;
			this.max = max;
			this.available = available;
			this.need = needTableHandle();
			this.semaphore = semaphore;
			this.runList = runList;
			this.deadlockList = deadlockList;
		}

		public void run() {
			int equalFlag = 0;

			while (countLoop <= 4) {
				try {
					semaphore.acquire();
					System.out.println("countLoop " + countLoop + " P[" + id + "]");
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					for (int i = 0; i < need.length; i++) {
						if (need[i] <= available.getAvailable(i)) {
							equalFlag++;
						} else {
							equalFlag = 0;
							break;
						}
					}
					if (equalFlag == need.length) {
						countLoop = 0;
						runList.put(raceCondition, "[P" + id + "]");
						deadlockList.remove(id);
						raceCondition++;
						for (int i = 0; i < need.length; i++) {
							available.setAvailable(i, available.getAvailable(i) + allocation[i]);
						}
						break;
					} else {
						if (!deadlockList.containsKey(id))
							deadlockList.put(id, "[P" + id + "]");
						countLoop++;
						semaphore.release();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("P[" + id + "] DONE");
			for (int i = 0; i < available.getAvailableSize(); i++)
				System.out.println(available.getAvailable(i));
			semaphore.release();
		}

		private int[] needTableHandle() {
			int[] makeTable = new int[available.getAvailableSize()];
			for (int i = 0; i < makeTable.length; i++) {
				makeTable[i] = this.max[i] - this.allocation[i];
			}
			return makeTable;
		}

		public int getNeed(int i) {
			return need[i];
		}

		public int getMax(int i) {
			return max[i];
		}

		public int getAllocation(int i) {
			return allocation[i];
		}

		public void setAllocation(int i, int value) {
			allocation[i] = value;
		}

		public int getId() {
			return id;
		}
	}

	static class Available {
		private int[] available;

		public Available(int[] available) {
			this.available = available;
		}

		public synchronized void setAvailable(int i, int value) {
			available[i] = value;
		}

		public synchronized int getAvailable(int i) {
			return available[i];
		}

		public int getAvailableSize() {
			return available.length;

		}
	}
}
