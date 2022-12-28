
#include <iostream>

using namespace std;

unsigned int toInt(string &in) {
  int bit0 = in[3] == '1' ? 1:0;
  int bit1 = in[2] == '1' ? 1:0;
  int bit2 = in[1] == '1' ? 1:0;
  int bit3 = in[0] == '1' ? 1:0;
  return 8*bit3 + 4*bit2 + 2*bit1 + 1*bit0;
}

// helper: get c-instruction binary code
int main() {

  string a, c1, c2, c3, c4, c5, c6, buf;
  while (cin >> buf) {
    if (buf.length() == 1 and a == "q")
      return 0;
    a = buf[0];
    c1 = buf[1];
    c2 = buf[2];
    c3 = buf[3];
    c4 = buf[4];
    c5 = buf[5];
    c6 = buf[6];

    string o3 = "000" + a;
    string o2 = c1 + c2 + c3 + c4;
    string o1 = c5 + c6 + "00";
    char o = '0';
    cout << std::hex << toInt(o3) << toInt(o2) <<toInt(o1)
          << o <<endl;
    continue;
  }
  return 0;
}