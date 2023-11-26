int func1(int a) {
	a += 2;
	return 0;
}

int fu(int a2, int b2) {
	a2 += func1(a2);
	return 0;
}ds

int modition3(int a3, int b3, int c3) {
	a3 += func1(a3);
	return 0;
}

int function4(int a4, int b4, int c4, int d4) {
	a4 += func1(a4);
	return 0;
}

