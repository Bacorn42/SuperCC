package io;

import emulator.Solution;
import game.Level;
import util.ByteList;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;

public class TWSWriter{
    
    public static void write(File twsFile, Level level, Solution solution, ByteList mouseMoves) {
        try(TWSOutputStream writer = new TWSOutputStream(twsFile)) {
            writer.writeTWSHeader(level);
            writer.writeInt(writer.solutionLength(solution));
            writer.writeLevelHeader(level, solution);
            int timeBetween = 0;
            boolean firstMove = true;
            int i = 0;
            for (byte b : solution.halfMoves) {
                if (b == '-') timeBetween += 2;
                else {
                    int relativeClickX;
                    int relativeClickY;
                    int twsRelativeClick = 0;
                    if (b <= 0) {
                        relativeClickX = mouseMoves.get(i++); //Postfix ++ returns the current value of i then increments it
                        relativeClickY = mouseMoves.get(i++);

                        twsRelativeClick = 16 + ((relativeClickY + 9) * 19) + (relativeClickX + 9);
                    }
                    writer.writeMove(b, timeBetween, firstMove, twsRelativeClick);
                    timeBetween = 2;
                    firstMove = false;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class TWSOutputStream extends FileOutputStream{

        private final byte UP = 3, LEFT = 7, DOWN = 11, RIGHT = 15;

        //all key directions (u, l, d, r) use format 2 on this page http://www.muppetlabs.com/~breadbox/software/tworld/tworldff.html#3

        void writeMove(byte b, int time, boolean firstMove, int relativeClick) throws IOException {
            if (!firstMove) time -= 1;
            byte twsMoveByte;
            boolean useFormat4 = false;
            switch (b) {
                case 'u': twsMoveByte = UP; break;
                case 'l': twsMoveByte = LEFT; break;
                case 'd': twsMoveByte = DOWN; break;
                case 'r': twsMoveByte = RIGHT; break;
                default:
                    twsMoveByte = 0; //turns out if you don't have this it won't compile due to twsMoveByte "may not have been initialized" despite that it'll never get used if this code block activates!
                    useFormat4 = true;
                    //Do all the things involving the type 4 mouse moves either here or next
            }
            if (!useFormat4) { //This is all format 2 which is all SuCC supports using for key moves
                writeFormat2(twsMoveByte, time);
            }
            else {
                writeFormat4(time, relativeClick);
            }
        }
        void writeTWSHeader (Level level) throws IOException {
            writeInt(0x999B3335);                        // Signature
            write(2);                                   // Ruleset
            writeShort(level.getLevelNumber());
            write(0);
        }
        void writeLevelHeader (Level level, Solution solution) throws IOException {

            int endingSlide = 0; //TODO: Refactor this, its not really appropriate here
            if (level.isCompleted() && level.getChip().isSliding()) endingSlide = 1; //Its an evil hack but i don't think there's any other way to resolve this, or anywhere i can move this

            writeShort(level.getLevelNumber());
            byte[] password = level.getPassword();
            for (int i = 0; i < 4; i++) write(password[i]);
            write(0);                                   // Other flags
            write(solution.step.toTWS());
            writeInt(solution.rngSeed);
            writeInt(2 * solution.halfMoves.length - 2 - endingSlide);
            /* minus 2 because the time value is always 2 extra for unknown reasons (likely tick counting differences between TW and SuCC).
            endingSlide because if Chip ends up sliding into an exit it doesn't tick the timer,
            however TW (which moves in 1/4 steps even in MS) does tick the quarter step timer,
            so we have to subtract one here whenever this situation happens */
        }
        private static final int LEVEL_HEADER_SIZE = 16;
    
        void writeShort(int i) throws IOException{
            write(i);
            write(i >> 8);
        }
        void writeInt(int i) throws IOException{
            write(i);
            write(i >> 8);
            write(i >> 16);
            write(i >> 24);
        }
        public TWSOutputStream(File file) throws IOException {
            super(file);
        }
        public int solutionLength(Solution s) {
            int c = LEVEL_HEADER_SIZE;
            for (byte b : s.halfMoves) if (b != '-') c += 4;
            return c;
        }
        void writeFormat2(byte twsMoveByte, int time) throws IOException {
            write(twsMoveByte | (time & 0b111) << 5);
            write((time >> 3) & 0xFF);
            write((time >> 11) & 0xFF);
            write((time >> 19) & 0xFF);
        }
        void writeFormat4(int time, int direction) throws IOException {
            // First byte DDD1NN11
            //int numBytes = measureTime(time);
            write(0b1_0011 | (2 << 2) | (direction & 0b111) << 5); //(2 << 2), the first 2 used to be the result of measureTime however as all bytes have to be 4 long its forced to being 2 now

            // Second byte TTDDDDDD
            write (((time & 0b11) << 6) | ((direction & 0b11_1111_000) >> 3));

            // Third byte TTTTTTTT
            //if (numBytes > 0) {
                write ((time & 0b1111_1111_00) >> 2);
            //}

            // Fourth byte TTTTTTTT
            //if (numBytes > 1) {
                write ((time & 0b1111_1111_0000_0000_00) >> 10);
            //}

            // Fifth byte 000TTTTT
//            if (numBytes > 2) {
//                write ((time & 0b1_1111_0000_0000_0000_0000_00) >> 18);
//            }
        }
//        int measureTime(int time) {
//            if ((time & 0b11) == time) {
//                return 0;
//            }
//            else if ((time & 0b11_1111_1111) == time) {
//                return 1;
//            }
//            else if ((time & 0b11_1111_1111_1111_1111) == time) {
//                return 2;
//            }
//            else {
//                return 3;
//            }
//        }
    }
}
