package game;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class SaveState {
    
    public static final int NO_CLICK = 1025;
    private static final int RLE_MULTIPLE = 0x7F;
    private static final int RLE_END = 0x7E;

    Tile[] layerBG;
    Tile[] layerFG;
    Creature chip;
    int timer;
    int chipsLeft;
    short[] keys;
    short[] boots;
    RNG rng;
    int mouseClick;
    BitSet traps;
    CreatureList monsterList;
    SlipList slipList;

    public byte[] save(){
        byte[] traps = this.traps.toByteArray();
        SavestateWriter writer = new SavestateWriter();
        writer.writeShort(3);                                        // Version
        writer.writeShort((short) chip.bits());
        writer.writeLayer(layerBG);
        writer.writeLayer(layerFG);
        writer.writeShort(timer);
        writer.writeShort(chipsLeft);
        writer.writeShorts(keys);
        writer.writeShorts(boots);
        writer.writeInt(rng.currentValue);
        writer.writeShort(mouseClick);
        writer.writeShort(traps.length);
        writer.writeBytes(traps);
        writer.writeShort(monsterList.list.length);
        writer.writeMonsterArray(monsterList.list);
        writer.writeShort(slipList.size());
        writer.writeMonsterList(slipList);
        
        return writer.toByteArray();
    }
    
    public static Creature getChip(byte[] savestate){
        return new Creature(((savestate[2] & 0xFF) << 8) | (savestate[3] & 0xFF));
    }

    public void load(byte[] savestate){
        SavestateReader reader = new SavestateReader(savestate);
        int version = reader.readShort();
        chip = new Creature(reader.readShort());
        layerBG = reader.readLayer(32*32);
        layerFG = reader.readLayer(32*32);
        timer = (short) reader.readShort();
        chipsLeft = reader.readShort();
        keys = reader.readShorts(4);
        boots = reader.readShorts(4);
        rng.currentValue = (reader.readInt());
        mouseClick = reader.readShort();
        traps = BitSet.valueOf(reader.readBytes(reader.readShort()));
        monsterList.list = reader.readMonsterArray(reader.readShort());
        slipList.setSliplist(reader.readMonsterArray(reader.readShort()));
    }

    public SaveState(Tile[] layerBG, Tile[] layerFG, CreatureList monsterList, SlipList slipList, Creature chip,
                     int timer, int chipsLeft, short[] keys, short[] boots, RNG rng, int mouseClick, BitSet traps){
        this.layerBG = layerBG;
        this.layerFG = layerFG;
        this.monsterList = monsterList;
        this.slipList = slipList;
        this.chip = chip;
        this.timer = timer;
        this.chipsLeft = chipsLeft;
        this.keys = keys;
        this.boots = boots;
        this.rng = rng;
        this.mouseClick = mouseClick;
        this.traps = traps;
    }

    private class SavestateReader extends ByteArrayInputStream{

        int readUnsignedByte(){
            return read() & 0xFF;
        }
        int readShort(){
            int n = readUnsignedByte() << 8;
            return n | readUnsignedByte();
        }
        int readInt(){
            int n = readUnsignedByte() << 24;
            n |= readUnsignedByte() << 16;
            n |= readUnsignedByte() << 8;
            return n | readUnsignedByte();
        }
        byte[] readBytes(int length){
            byte[] out = new byte[length];
            for (int i = 0; i < length; i++){
                out[i] = (byte) read();
            }
            return out;
        }
        short[] readShorts(int length){
            short[] out = new short[length];
            for (int i = 0; i < length; i++){
                out[i] = (short) readShort();
            }
            return out;
        }
        Tile[] readLayer(int length){
            Tile[] layer = new Tile[32*32];
            int tileIndex = 0;
            int b;
            while ((b = read()) != RLE_END){
                if (b == RLE_MULTIPLE){
                    int rleLength = readUnsignedByte() + 1;
                    Tile t = Tile.fromOrdinal(read());
                    for (int i = 0; i < rleLength; i++){
                        layer[tileIndex++] = t;
                    }
                }
                else layer[tileIndex++] = Tile.fromOrdinal(b);
            }
            return layer;
        }
        Creature[] readMonsterArray(int length){
            Creature[] monsters = new Creature[length];
            for (int i = 0; i < length; i++){
                monsters[i] = new Creature(readShort());
            }
            return monsters;
        }
        ArrayList<Creature> readMonsterList(int length){
            ArrayList<Creature> monsters = new ArrayList<Creature>();
            for (int i = 0; i < length; i++){
                monsters.add(new Creature(readShort()));
            }
            return monsters;
        }

        public SavestateReader(byte[] b){
            super(b);
        }

    }

    private class SavestateWriter extends ByteArrayOutputStream{

        void writeShort(int n){
            write(n >>> 8);
            write(n);
        }
        void writeInt(int n){
            write(n >>> 24);
            write(n >>> 16);
            write(n >>> 8);
            write(n);
        }
        /*
        public void writeBytes(byte[] a){
            for (int i = 0; i < a.length; i++){
                write(a[i]);
            }
        }
        */
        void writeShorts(short[] a){
            for (int i = 0; i < a.length; i++){
                writeShort(a[i]);
            }
        }
        void writeLayer(Tile[] layer){
            int lastOrdinal = layer[0].ordinal();
            int ordinal;
            int copyCount = -1;
            for (Tile t : layer){
                ordinal = t.ordinal();
                if (ordinal == lastOrdinal){
                    if (copyCount == 255){
                        write(RLE_MULTIPLE);
                        write(copyCount);
                        copyCount = 0;
                        write(ordinal);
                    }
                    else copyCount++;
                }
                else {
                    if (copyCount != 0){
                        write(RLE_MULTIPLE);
                        write(copyCount);
                    }
                    write(lastOrdinal);
                    copyCount = 0;
                    lastOrdinal = ordinal;
                }
            }
            if (copyCount != 0){
                write(RLE_MULTIPLE);
                write(copyCount);
            }
            write(lastOrdinal);
            write(RLE_END);
        }
        void writeMonsterArray(Creature[] monsters){
            for (Creature monster : monsters) writeShort(monster.bits());
        }
        void writeMonsterList(List<Creature> monsters){
            for (Creature monster : monsters) writeShort(monster.bits());
        }

    }

}