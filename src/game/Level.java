package game;

import game.button.*;

import java.util.BitSet;

import static game.Tile.*;

public class Level extends SaveState {
    
    private static final int HALF_WAIT = 0, KEY = 1, CLICK_EARLY = 2, CLICK_LATE = 3;
    public static final byte UP = 'u', LEFT = 'l', DOWN = 'd', RIGHT = 'r', WAIT = '-';
    
    public final int levelNumber, startTime;
    public final byte[] title, password, hint;
    public final short[] toggleDoors, portals;
    private GreenButton[] greenButtons;
    private RedButton[] redButtons;
    private BrownButton[] brownButtons;
    private BlueButton[] blueButtons;

    private int rngSeed;
    private Step step;

    public Level(int levelNumber, byte[] title, byte[] password, byte[] hint, short[] toggleDoors, short[] portals,
                 GreenButton[] greenButtons, RedButton[] redButtons,
                 BrownButton[] brownButtons, BlueButton[] blueButtons, BitSet traps,
                 Layer layerBG, Layer layerFG, CreatureList monsterList, SlipList slipList,
                 Creature chip, int time, int chips, RNG rng, int rngSeed, Step step){

        super(layerBG, layerFG, monsterList, slipList, chip,
                time, chips, new short[4], new short[4], rng, NO_CLICK, traps);

        this.levelNumber = levelNumber;
        this.startTime = time;
        this.title = title;
        this.password = password;
        this.hint = hint;
        this.toggleDoors = toggleDoors;
        this.portals = portals;
        this.greenButtons = greenButtons;
        this.redButtons = redButtons;
        this.brownButtons = brownButtons;
        this.blueButtons = blueButtons;
        this.rngSeed = rngSeed;
        this.step = step;

        this.slipList.setLevel(this);
        this.monsterList.setLevel(this);
    }

    public Layer getLayerBG() {
        return layerBG;
    }
    public Layer getLayerFG() {
        return layerFG;
    }
    public int getTimer(){
        if (tickNumber == 0) return startTime;
        else return startTime - tickNumber + 1;                     // The first tick does not change the timer
    }
    public int getTChipTime() {
        if (tickNumber == 0) return 9999;
        else return 9999 - tickNumber + 1;                     // The first tick does not change the timer
    }
    public int getChipsLeft(){
        return chipsLeft;
    }
    public void setChipsLeft(int chipsLeft){
        this.chipsLeft = chipsLeft;
    }
    public Creature getChip(){
        return chip;
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
    public void setKeys(short[] keys){
        this.keys = keys;
    }
    public short[] getBoots(){
        return boots;
    }
    public void setBoots(short[] boots){
        this.boots = boots;
    }
    public CreatureList getMonsterList(){
        return monsterList;
    }
    public SlipList getSlipList(){
        return slipList;
    }
    public GreenButton[] getGreenButtons() {
        return greenButtons;
    }
    public RedButton[] getRedButtons() {
        return redButtons;
    }
    public BrownButton[] getBrownButtons() {
        return brownButtons;
    }
    public BlueButton[] getBlueButtons() {
        return blueButtons;
    }
    public BitSet getOpenTraps(){
        return traps;
    }
    public void setClick(int position){
        this.mouseClick = position;
    }
    
    protected void popTile(Position position){
        layerFG.set(position, layerBG.get(position));
        layerBG.set(position, FLOOR);
    }
    protected void insertTile(Position position, Tile tile){
        layerBG.set(position, layerFG.get(position));
        layerFG.set(position, tile);
    }
    
    Button getButton(Position position, Class buttonType) {
        Button[] buttons;
        if (buttonType.equals(GreenButton.class)) buttons = greenButtons;
        else if (buttonType.equals(RedButton.class)) buttons = redButtons;
        else if (buttonType.equals(BrownButton.class)) buttons = brownButtons;
        else if (buttonType.equals(BlueButton.class)) buttons = blueButtons;
        else throw new RuntimeException("Invalid class");
        for (Button b : buttons) {
            if (b.getButtonPosition().equals(position)) return b;
        }
        return null;
    }
    boolean isTrapOpen(Position position) {
        for (BrownButton b : brownButtons) {
            if (b.getTargetPosition().equals(position) && traps.get(b.getTrapIndex())) return true;
        }
        return false;
    }
    
    private int moveType(byte b, boolean halfMove, boolean chipSliding){
        if (b <= 0 || b == WAIT){
            if (mouseClick != NO_CLICK) {
                if (chipSliding) return CLICK_EARLY;
                if (halfMove) return CLICK_LATE;
            }
            else return HALF_WAIT;
        }
        else if (b == UP || b == LEFT || b == DOWN || b == RIGHT){
            return KEY;
        }
        return HALF_WAIT;
    }
    
    private void moveChipSliding(){
        int direction = chip.getDirection();
        Tile bgTile = layerBG.get(chip.getPosition());
        if (bgTile.isFF()) chip.tick(new int[] {direction}, this, true);
        else chip.tick(chip.getSlideDirectionPriority(bgTile, rng, true), this, true);
    }
    
    private void moveChip(int[] directions){
        int oldPosition = chip.getIndex();
        for (int direction : directions) {
            if (chip.isSliding()) {
                if (!layerBG.get(chip.getPosition()).isFF()) continue;
                if (direction == chip.getDirection()) continue;
            }
            chip.tick(new int[] {direction}, this, false);
            if (chip.getIndex() != oldPosition) break;
        }
    }

    private void finaliseTraps(){
        for (BrownButton b : brownButtons) {
            if (layerBG.get(b.getButtonPosition()) == BUTTON_BROWN){
                traps.set(b.getTrapIndex(), true);
            }
            else if (layerFG.get(b.getTargetPosition()) == TRAP){
                traps.set(b.getTrapIndex(), false);
            }
        }
    }

    private void initialiseSlidingMonsters(){
        for (Creature m : monsterList.list) m.setSliding(false);
        for (Creature m : slipList) m.setSliding(true);
    }
    
    // return: did it tick twice?
    public boolean tick(byte b, int[] directions){
        
        initialiseSlidingMonsters();
        tickNumber++;
        boolean isHalfMove = tickNumber % 2 == 0;
        int moveType = moveType(b, isHalfMove, chip.isSliding());
    
        if (tickNumber > 2 && !isHalfMove) monsterList.tick();
        
        if (chip.isDead()) return false;
        if (chip.isSliding()) moveChipSliding();
        if (chip.isDead()) return false;
        if (moveType == CLICK_EARLY) moveChip(chip.seek(new Position(mouseClick)));
        if (chip.isDead()) return false;
        slipList.tick();
        if (chip.isDead()) return false;
        if (moveType == KEY) moveChip(directions);
        else if (moveType == CLICK_LATE) moveChip(chip.seek(new Position(mouseClick)));
        if (chip.isDead()) return false;

        monsterList.finalise();
        finaliseTraps();
        if (moveType == KEY || chip.getIndex() == mouseClick) mouseClick = NO_CLICK;
    
        return moveType == KEY && !isHalfMove && !chip.isSliding();
    }
    
}
