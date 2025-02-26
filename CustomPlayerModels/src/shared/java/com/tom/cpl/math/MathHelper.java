package com.tom.cpl.math;

public class MathHelper {
	public static double clamp(double num, double min, double max) {
		if (num < min) {
			return min;
		} else {
			return num > max ? max : num;
		}
	}

	/**
	 * Returns the value of the first parameter, clamped to be within the lower and upper limits given by the second and
	 * third parameters.
	 */
	public static int clamp(int num, int min, int max) {
		if (num < min) {
			return min;
		} else {
			return num > max ? max : num;
		}
	}

	public static int ceil(float value) {
		int i = (int)value;
		return value > i ? i + 1 : i;
	}

	public static int ceil(double value) {
		int i = (int)value;
		return value > i ? i + 1 : i;
	}
}
