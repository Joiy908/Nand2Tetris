//
// Created by Joiy on 2022/12/26.
//

#include <iostream>
#include <fstream>
#include <sstream>
#include <cctype>
#include <map>
#include <bitset>
#include <algorithm>

#include "Assembler.h"

using namespace std;

// remove comments and whitespaces
void filterComment(ifstream &in, stringstream &out);

/**
 * 1 extract user-defined branch-labels, add to table
 * 2 extract user-defined variable, allot it a "register", add to table
 * 3 \@labels => @33 like
 */
void filterLabel(stringstream &in, stringstream &out);

/**
 * code without labels to binary
 */
void toBinary(stringstream &in, ofstream &out);

/**
 * read uint16_t from binary file stream @param in
 *  convert it to '1's and '0's, write to @param out
 * @param in
 * @param out
 */
void toChars(ifstream &in, ofstream &out);

// ===== helpers

bool isNum(string &s);

/**
 * parse a C instruction to tuple<dest, comp, jump>
 */
tuple<string, string, string> parseC(string in);

/**
 * Xxx.asm to Xxx.hack
 */
string getHackFilePath(const string &asmPath);

/**
 * Xxx.hack to XxxChar.hack
 */
string getCharFilePath(const string &hackName);

void checkFileIsOpen(ios &f, string &fName);

int main(int argc, char **args) {
  if (argc == 1) {
    cerr << "usage: "<< args[0] << " xxx.asm" << endl;
    return -1;
  }
  string filePath = args[1];

  ifstream in(filePath);
  checkFileIsOpen(in, filePath);

  cout << filePath << "is open, filtering the comments..." << endl;
  stringstream codeWithoutComment;
  filterComment(in, codeWithoutComment);

  cout << "the comment is removed, filtering the label..." << endl;
  stringstream stdCode;
  filterLabel(codeWithoutComment, stdCode);

  ofstream out;
  string hackFilePath = getHackFilePath(filePath);
  out.open(hackFilePath, ios::out | ios::binary);
  checkFileIsOpen(out, hackFilePath);

  cout << "the label is converted to digit, converting to binary..." << endl;
  toBinary(stdCode, out);

  cout << "binary file is writen to " << hackFilePath <<
       " ,with size : " << out.tellp() << " bytes" << endl;
  out.close();


  ifstream bIn;
  bIn.open(hackFilePath, ios::in | ios::binary);
  checkFileIsOpen(bIn, hackFilePath);

  cout << "reading binary file, and converting to \"binary\" characters..." << endl;
  string charFName = getCharFilePath(hackFilePath);
  ofstream outChar(charFName);
  checkFileIsOpen(outChar, charFName);

  toChars(bIn, outChar);
  cout << "txt file is writen to " << getCharFilePath(hackFilePath) << endl;
}

// remove comments and whitespaces
void filterComment(ifstream &in, stringstream &out) {
  State flag = NEWLINE;
  char c;
  while (in.get(c)) {
    switch (flag) {
      case NEWLINE:
        if (c == '/') {
          flag = COMMENT;
        } else if (c == '@' or isalnum(c)) {
          flag = INSTRUCTION;
          out << c;
        } else if (c == '(') {
          flag = LABEL_DEFINE;
          out << c;
        }// else continue;
        break;
      case COMMENT:
        if (c == '\n') {
          flag = NEWLINE;
        }// else continue;
        break;
      case INSTRUCTION:
        if (c == '/') {
          flag = COMMENT;
          out << endl;
        } else if (c == '\n') {
          flag = NEWLINE;
          out << c;
        } else if (iscntrl(c) or c == ' ') {
          continue;
        } else {
          out << c;
        }
        break;
      case LABEL_DEFINE:
        if (c == '\n') {
          flag = NEWLINE;
        } else if (iscntrl(c)) {
          continue;
        }
        out << c;
        break;
    }
  }
}

void filterLabel(stringstream &in, stringstream &out) {
  map<string, string> table;
  for (auto &p: PRE_DEFINED_KEYS)
    table.insert(p);

  long lineNum = 0;
  string buf;
  stringstream temp;
  while (getline(in, buf) && !buf.empty()) {
    if (buf[0] == '(') {
      string label = buf.substr(1, buf.length() - 2);
      table.insert({label, to_string(lineNum)});
      continue;
    }
    temp << buf << endl;
    lineNum++;
  }

  int tempRNum = 16;
  while (getline(temp, buf) and !buf.empty()) {
    if (buf[0] == '@') {
      string addr = buf.substr(1, string::npos);
      if (!isNum(addr)) {
        auto it = table.find(addr);
        if (it != table.end()) {
          out << '@' << it->second << endl;
        } else { // is a variable
          table.insert({addr, to_string(tempRNum)});
          out << '@' << tempRNum << endl;
          tempRNum++;
        }
        continue;
      }
    }//  else {
    out << buf << endl;
  }
}

void toBinary(stringstream &in, ofstream &out) {
  map<string, uint16_t> destMap;
  map<string, uint16_t> compMap;
  map<string, uint16_t> jumpMap;
  // init 3 map
  for (auto &p: DEST_MAP)
    destMap.insert(p);
  for (auto &p: COMP_MAP)
    compMap.insert(p);
  for (auto &p: JUMP_MAP)
    jumpMap.insert(p);

  string buf;
  uint16_t bData;
  while (getline(in, buf) and !buf.empty()) {
    if (buf[0] == '@') {
      stringstream ss(buf.substr(1, buf.length() - 1));
      ss >> bData;
    } else {
      auto token = parseC(buf);
      auto dest = destMap.find(get<0>(token));
      if (dest == destMap.end()) {
        cerr << buf << "[0] is not find!" << endl;
        return;
      }
      auto comp = compMap.find(get<1>(token));
      if (comp == compMap.end()) {
        cerr << buf << "[1] is not find!" << endl;
        return;
      }
      auto jump = jumpMap.find(get<2>(token));
      if (jump == jumpMap.end()) {
        cerr << buf << "[2] is not find!" << endl;
        return;
      }
      bData = static_cast<uint16_t>(0xe000) | (dest->second | (comp->second | jump->second));
    }
    out.write(reinterpret_cast<char *>(&bData), sizeof(uint16_t));
  }
}

void toChars(ifstream &in, ofstream &out) {
  uint16_t bWord;

  while (in.read(reinterpret_cast<char *>(&bWord), sizeof(uint16_t))) {
    std::bitset<16> cWord(bWord);
    out << cWord << endl;
  }
}

// ==== helpers
bool isNum(string &s) {
  return std::all_of(s.begin(), s.end(),
                     [](char c) { return isdigit(c); });
}

string getCharFilePath(const string &hackName) {
  size_t pos = hackName.find_last_of('.');
  string s = hackName;
  s.insert(pos, "Char");
  return s;
}

string getHackFilePath(const string &asmPath) {
  string s = asmPath;
  size_t pos = s.find_last_of('.');
  s.erase(pos, 12);
  s += ".hack";
  return s;
}

tuple<string, string, string> parseC(string in) {
  tuple<string, string, string> rst;
  auto pos1 = in.find(';');
  if (pos1 != string::npos) {
    get<2>(rst) = in.substr(pos1 + 1, string::npos);
    in.erase(pos1, string::npos);
  }
  auto pos2 = in.find('=');
  if (pos2 != string::npos) {
    get<0>(rst) = in.substr(0, pos2);
    in.erase(0, pos2 + 1);
  }
  get<1>(rst) = in;
  return rst;
}

void checkFileIsOpen(ios &f, string &fName) {
  if (f.fail()) {
    cerr << "fail to open the file " << fName << ":\n";
    switch (errno) {
      case EACCES:
        // this is set if the drive is not ready in DOS
        cerr << "    Drive not ready or permission denied" << endl;
        break;
      case ENOENT:
        cerr << "    Could not find this file " << endl;
        break;
      default:
        perror("    opening data file");
    }
    exit(EXIT_FAILURE);
  }
}

