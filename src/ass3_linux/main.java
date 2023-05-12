package ass3_linux;
/*
 * Javier Giberg
 */
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class main {

	public static void main(String[] args) {
		Semaphore semaphore = new Semaphore(0, true);
		HashMap<Integer, String> RunList = new HashMap<>();
		HashMap<Integer, String> DeadlockList = new HashMap<>();
		/*
		 * install params with I/O
		 */
		char character = 'A';
		Scanner sc = new Scanner(System.in);
		int[] createAvailable = { 0, 0, 0, 0, 0 };
		System.out.println("Enter Available array: ");
		for (int i = 0; i < createAvailable.length; i++) {
			System.out.println("Enter Available [" + character + "] ");
			createAvailable[i] = sc.nextInt();
			character++;
		}
		Available available = new Available(createAvailable);
		System.out.println("Enter num of process: ");
		P[] processTable = new P[sc.nextInt()];

		for (int i = 0; i < processTable.length; i++) {
			System.out.println("Handle [P" + (i + 1) + "] Matixs'");
			character = 'A';
			int[] Allocation = { 0, 0, 0, 0, 0 };
			int[] Max = { 0, 0, 0, 0, 0 };
			for (int j = 0; j < Allocation.length; j++) {
				System.out.println("Enter Allocation [" + character + "] :");
				Allocation[j] = sc.nextInt();
				System.out.println("Enter Max [" + character + "] :");
				Max[j] = sc.nextInt();
				character++;
			}
			processTable[i] = new P(available, Max, Allocation, semaphore, RunList, DeadlockList);
			;
		}
		semaphore.release(1);
		for (P process : processTable)
			new Thread(process).start();
		try {
			Thread.currentThread().sleep(2500);;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.print("Run Process <");
		for (int i = 0; i < RunList.size(); i++) {
			System.out.print(" " + RunList.get(i) + " ,");
		}
		System.out.print(">");
		System.out.println();
		System.out.println("---------------------------");
		System.out.print("Deadlock Process <");
		for (Integer item : DeadlockList.keySet()) {
			System.out.print(" " + item + " ,");
		}
		System.out.print(" >");

	}
	/*
	 * classe P (Process)
	 */

	static class P implements Runnable {
		private static int count = 1;
		private static int countLoop = 0;
		private static int raceCondition = 0;
		private int id;;
		private int[] Need;
		private Available available;
		private int[] Max;
		private int[] Allocation;
		Semaphore semaphore;
		HashMap<Integer, String> RunList;
		HashMap<Integer, String> DeadlockList;

		public P(Available available, int[] Max, int[] Allocation, Semaphore semaphore,
				HashMap<Integer, String> RunList, HashMap<Integer, String> DeadlockList) {
			this.id = count;
			count++;
			this.Allocation = Allocation;
			this.Max = Max;
			this.available = available;
			this.Need = needTableHandle();
			this.semaphore = semaphore;
			this.RunList = RunList;
			this.DeadlockList = DeadlockList;
		}

		// -----------------------Run block------------------------------------------
		public void run() {
			int equalFlag = 0;

			while (countLoop<=4) {
				try {
					semaphore.acquire();
					System.out.println("countLoop " + countLoop + " P[" + id + "]");
					try {
						Thread.currentThread().sleep(250);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					for (int i = 0; i < Need.length; i++) {
						if (Need[i] <= available.getAvailable(i)) {
							equalFlag++;
						} else {
							equalFlag = 0;
							break;
						}
					}
					if (equalFlag == Need.length) {
						countLoop = 0;
						RunList.put(raceCondition, "[P" + id + "]");
						DeadlockList.remove(id);
						raceCondition++;
						for (int i = 0; i < Need.length; i++) {
							available.setAvailable(i, available.getAvailable(i) + Allocation[i]);
						}

						break;
					} else {
						if (!DeadlockList.containsKey(id))
							DeadlockList.put(id, "[P" + id + "]");
						countLoop++;
						semaphore.release();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
//				
			}
			System.out.println("P[" + id + "] DONE");
			for (int i = 0; i < available.getAvailableSize(); i++)
				System.out.println(available.getAvailable(i));
			semaphore.release();

		}

		// -----------------------Run block------------------------------------------
		/*
		 * make Need Table
		 */
		private int[] needTableHandle() {
			int[] makeTable = new int[available.getAvailableSize()];
			for (int i = 0; i < makeTable.length; i++) {
				makeTable[i] = this.Max[i] - this.Allocation[i];
			}
			return makeTable;
		}

		public int getNeed(int i) {
			return Need[i];
		}

		public int getMax(int i) {
			return Max[i];
		}

		public int getAllocation(int i) {
			return Allocation[i];
		}

		public void setAllocation(int i, int value) {
			Allocation[i] = value;
		}

		public int getId() {
			return id;
		}

	}
	/*
	 * class Available ( share resource )
	 */

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

//		public synchronized void setAvailable(int[] available) {
//			this.available = available;
//		}

		public int getAvailableSize() {
			return available.length;
		}

	}

}
