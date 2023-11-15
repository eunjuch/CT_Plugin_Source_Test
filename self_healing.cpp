//#include "gmock/gmock.h"
//#include "gtest/gtest.h"


class Turtle {
public:
	virtual ~Turtle() {}

	virtual void PenUp() = 0;
	virtual void PenDown() = 0;
	virtual void Forward(int distance) = 0;
	virtual void Turn() = 0;
	virtual void Turn(int degrees) = 0;
	virtual void Turn(char degrees) = 0;
	virtual void GoTo(int x, int y) = 0;
	virtual int GetX() const = 0;
	virtual int GetY() const { return 1;}
	int GetAB() const { return 2; }
	int GetXY() const { return 500; }
};


int c_modi= 5;
class Painter {
public:
	Painter(Turtle* turtle) : turtle_(turtle) {

		int a = 0;
		int b = -12;
		if (turtle->GetX() > b) {
			a++;
		}
	}

	bool drawCircle() {
		return turtle_->GetX() == 0;

	}

	int drawY() {
		return turtle_->GetY();
	}

	int drawAB() {
		return turtle_->GetAB();
	}

	int drawXY() {
		return turtle_->GetXY();
	}



private:
	Turtle* turtle_;
};

class T1 {
public:
	virtual void func1(Turtle* a, Painter* b){}
};

/*
TEST(test1, sample1) {
	int tmp = 5;
	MockTurtle turtle;
	EXPECT_CALL(turtle, PenDown())                  // #3
		.Times(AtLeast(1));

	EXPECT_CALL(turtle, getObj()).WillRepeatedly(Return(Obj1(tmp)));

	EXPECT_CALL(turtle, GetXY()).WillRepeatedly(Return(100));

	auto x = turtle.GetX();
	auto b= turtle.getObj();
	auto c = turtle.GetXY();

	Painter painter(&turtle);

	Son s(&turtle);
	auto cc = s.drawCircle();

	EXPECT_TRUE(painter.drawCircle());
	EXPECT_EQ(painter.drawXY(), 100);
}
*/

class Concrete {
public:
	Concrete (int a) : intVar_(a) {}
	virtual int getInt() {return intVar_;}
private:
	int intVar_;
};

class Concrete2 {
public:
	Concrete2 (int a) : intVar_(a) {}
	int getInt() {return intVar_;}
private:
	int intVar_;
};
