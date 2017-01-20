package name.azzurite.mcserver;

import java.util.Scanner;

public class Input {

	private static Scanner input;

	static {
		if (System.console() != null) {
			input =  new Scanner(System.console().reader());
		} else {
			input = new Scanner(System.in);
		}
	}

	public static boolean getYesNo(String prompt) {
		String answer;
		do {
			System.out.print(prompt);
			System.out.println(" [Y/n]");

			answer = input.nextLine();
		} while (!answer.isEmpty() && !answer.equalsIgnoreCase("y") && !answer.equalsIgnoreCase("n"));

		return answer.isEmpty() || answer.equalsIgnoreCase("y");
	}

}
