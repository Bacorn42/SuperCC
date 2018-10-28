package game;

import emulator.SuperCC;

import java.util.BitSet;

import static game.Tile.*;

public class Level extends SaveState {
    
    private static final int HALF_WAIT = 0, KEY = 1, CLICK = 2;
    
    public final int levelNumber;
    public final byte[] title, password, hint;
    public final short[] toggleDoors, portals;
    public final int[][] trapConnections, cloneConnections;

    protected int tickN;
    private int rngSeed;
    private Step step;

    public Level(int levelNumber, byte[] title, byte[] password, byte[] hint, short[] toggleDoors, short[] portals,
                 int[][] trapConnections, BitSet traps, int[][] cloneConnections,
                 Tile[] layerBG, Tile[] layerFG, CreatureList monsterList, SlipList slipList,
                 Creature chip, int time, int chips, RNG rng, int rngSeed, Step step){

        super(layerBG, layerFG, monsterList, slipList, chip, new byte[0],
                time, chips, new short[4], new short[4], rng, NO_CLICK, traps);

        this.levelNumber = levelNumber;
        this.title = title;
        this.password = password;
        this.hint = hint;
        this.toggleDoors = toggleDoors;
        this.portals = portals;
        this.trapConnections = trapConnections;
        this.cloneConnections = cloneConnections;
        this.rngSeed = rngSeed;
        this.step = step;

        this.slipList.setLevel(this);
        this.monsterList.setLevel(this);
    }

    public Tile[] getLayerBG() {
        return layerBG;
    }
    public Tile[] getLayerFG() {
        return layerFG;
    }
    public int getTimer(){
        return timer;
    }
    public int getChipsLeft(){
        return chipsLeft;
    }
    public Creature getChip(){
        return chip;
    }
    public byte[] getMoves(){
        return moves;
    }
    public int getRngSeed(){
        return rngSeed;
    }
    public Step getStep(){
        return step;
    }
    public short[] getKeys(){
        return keys;
    }
    public short[] getBoots(){
        return boots;
    }
    public CreatureList getMonsterList(){
        return monsterList;
    }
    public SlipList getSlipList(){
        return slipList;
    }
    public int[][] getTrapConnections(){
        return trapConnections;
    }
    public int[][] getCloneConnections(){
        return cloneConnections;
    }
    public void setClick(int position){
        this.mouseClick = position;
    }
    
    protected Tile popTile(int position){
        layerFG[position] = layerBG[position];
        layerBG[position] = FLOOR;
        return layerFG[position];
    }
    protected Tile popTile(Position position){
        int index = position.getIndex();
        layerFG[index] = layerBG[index];
        layerBG[index] = FLOOR;
        return layerFG[index];
    }
    protected void insertTile(int position, Tile tile){
        layerBG[position] = layerFG[position];
        layerFG[position] = tile;
    }
    protected void insertTile(Position position, Tile tile){
        int index = position.getIndex();
        layerBG[index] = layerFG[index];
        layerFG[index] = tile;
    }
    
    private int moveType(byte b, boolean halfMove, boolean chipSliding){
        if (b <= 0 || b == SuperCC.WAIT){
            if (mouseClick != NO_CLICK && (chipSliding || halfMove)) return CLICK;
            else return HALF_WAIT;
        }
        else if (b == SuperCC.UP || b == SuperCC.LEFT || b == SuperCC.DOWN || b == SuperCC.RIGHT){
            return KEY;
        }
        else return HALF_WAIT;
    }

    public void tickTimer(int t){
        timer -= t;
        while (timer < -10) timer += 4;
    }

    private void moveChipSliding(){
        int direction = chip.getDirection();
        if (layerBG[chip.getIndex()].isFF()) chip.tick(new int[] {direction}, this);
        else chip.tick(new int[] {direction, Creature.turnFromDir(direction, Creature.TURN_AROUND)}, this);
    }
    
    private void moveChip(int[] directions){
        for (int direction : directions) {
            if (chip.isSliding()) {
                if (!layerBG[chip.getIndex()].isFF()) continue;
                if (direction == chip.getDirection()) continue;
            }
            chip.tick(new int[] {direction}, this);
        }
    }

    private void addMove(byte b){
        byte[] newMoves = new byte[moves.length+1];
        System.arraycopy(moves, 0, newMoves, 0, moves.length);
        newMoves[moves.length] = b;
        moves = newMoves;
    }

    private void finaliseTraps(){
        for (int i = 0; i < trapConnections.length; i++){
            if (layerBG[trapConnections[i][0]] == BUTTON_BROWN){
                traps.set(i, true);
            }
            else if (layerBG[trapConnections[i][1]] != TRAP){
                traps.set(i, false);
            }
        }
    }

    private void initialiseSlidingMonsters(){
        for (Creature m : monsterList.list) m.setSliding(false);
        for (Creature m : slipList) m.setSliding(true);
    }

    private static byte capital(byte b){
        if (b == '-') return '_';
        return (byte) Character.toUpperCase((char) b);
    }

    private int numTicks(byte newMove){
        int t = 0;
        for (byte c : moves){
            if (Character.isUpperCase(c)) t += 2;
            else t++;
        }
        if (Character.isUpperCase(newMove)) t += 2;
        else t++;
        return t;
    }
    
    // return: did it tick twice?
    public boolean tick(byte b, int[] directions){
        
        initialiseSlidingMonsters();
        tickN = numTicks(b);
        boolean isHalfMove = tickN % 2 == 0;
        int moveType = moveType(b, isHalfMove, chip.isSliding());

        if (tickN > 2){
            tickTimer(1);
            if (!isHalfMove) monsterList.tick();
        }
        
        if (chip.isDead()) return false;
        if (chip.isSliding()) moveChipSliding();
        if (chip.isDead()) return false;
        if (chip.isSliding() && !layerBG[chip.getIndex()].isFF()) b = SuperCC.WAIT;
        if (moveType == CLICK) moveChip(chip.seek(new Position(mouseClick)));
        if (chip.isDead()) return false;
        slipList.tick();
        if (chip.isDead()) return false;
        if (moveType == KEY) moveChip(directions);
        if (chip.isDead()) return false;

        monsterList.finalise();
        finaliseTraps();
        if (moveType == KEY || chip.getIndex() == mouseClick) mouseClick = NO_CLICK;

        if (moveType == KEY && !isHalfMove && !chip.isSliding()){
            tick(capital(b), null);
            return true;
        }
        addMove(b);
        return false;
    }
    
}
