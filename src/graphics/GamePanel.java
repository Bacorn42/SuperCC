package graphics;

import emulator.SuperCC;
import game.*;
import org.w3c.dom.css.RGBColor;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import static graphics.MainWindow.TILE_SIZE;

class GamePanel extends JPanel{

    // The background image
    private BufferedImage bg = new BufferedImage(32*TILE_SIZE, 32*TILE_SIZE, BufferedImage.TYPE_4BYTE_ABGR);
    // The foreground image
    private BufferedImage fg = new BufferedImage(32*TILE_SIZE, 32*TILE_SIZE, BufferedImage.TYPE_4BYTE_ABGR);
    // The image behind the background (32*32 floor tiles)
    private BufferedImage bbg = new BufferedImage(32*TILE_SIZE, 32*TILE_SIZE, BufferedImage.TYPE_4BYTE_ABGR);

    private Tile[] previousFG = new Tile[32*32];
    private SuperCC emulator;
    private boolean showMonsterList = false;
    private boolean showSlipList = false;

    // All 7*16 tile types are preloaded and stored here for fast access.
    private static final int CHANNELS = 4;
    private static final int SMALL_NUMERAL_WIDTH = 3, SMALL_NUMERAL_HEIGHT = 5;
    private static final int[][] tileImage = new int[7*16][TILE_SIZE*TILE_SIZE*CHANNELS];
    private static final int[][] blackDigits = new int[10][(SMALL_NUMERAL_WIDTH+2)*(SMALL_NUMERAL_HEIGHT+2)*CHANNELS];
    private static final int[][] blueDigits = new int[10][(SMALL_NUMERAL_WIDTH+2)*(SMALL_NUMERAL_HEIGHT+2)*CHANNELS];
    
    /**
     * Draw the game state. This does not update the graphics - call
     * updateGraphics first.
     * @param g The graphics to draw
     */
    @Override
    public void paintComponent(Graphics g){
        g.drawImage(bbg, 0, 0, null);
        g.drawImage(bg, 0, 0, null);
        g.drawImage(fg, 0, 0, null);
    }
    
    /**
     * Update the graphics using a new Level state. This does not redraw the
     * graphics.
     * @param level The level state to draw
     * @param fromScratch Whether to recreate the entire image (slow) or only
     *                    update where the level changed.
     */
    void updateGraphics(Level level, boolean fromScratch){

        Tile[] layerBG;
        Tile[] layerFG;

        try{
            layerFG = level.getLayerFG();
            layerBG = level.getLayerBG();
        }
        catch (NullPointerException npe){
            return;
        }

        WritableRaster rasterFG = fg.getRaster();
        WritableRaster rasterBG = bg.getRaster();

        for (int i = 0; i < 32*32; i++){
            if (fromScratch | layerFG[i] != previousFG[i]){
                int x = TILE_SIZE*(i%32), y = TILE_SIZE*(i/32);
                rasterBG.setPixels(x, y, TILE_SIZE, TILE_SIZE, tileImage[layerBG[i].ordinal()]);
                rasterFG.setPixels(x, y, TILE_SIZE, TILE_SIZE, tileImage[layerFG[i].ordinal()]);
            }
        }
        previousFG = layerFG.clone();
        if (showMonsterList) drawMonsterList(level.getMonsterList().list);
        if (showSlipList) drawSlipList(level.getSlipList());
    }
    
    private void drawMonsterList(Creature[] monsterList){
        WritableRaster raster = fg.getRaster();
        for (int i = 0; i < monsterList.length; i++){
            Creature monster = monsterList[i];
            int x = monster.getX()*TILE_SIZE, y = monster.getY()*TILE_SIZE;
            char[] chars = String.valueOf(i).toCharArray();
            for (int j = 0; j < chars.length; j++){
                int digit = Character.digit(chars[j], 10);
                raster.setPixels(x+4*j, y, SMALL_NUMERAL_WIDTH+2, SMALL_NUMERAL_HEIGHT+2, blackDigits[digit]);
            }
        }
    }
    
    private void drawSlipList(SlipList slipList){
        WritableRaster raster = fg.getRaster();
        int yOffset = TILE_SIZE - SMALL_NUMERAL_HEIGHT - 2;
        for (int i = 0; i < slipList.size(); i++){
            Creature monster = slipList.get(i);
            int x = monster.getX()*TILE_SIZE, y = monster.getY()*TILE_SIZE;
            char[] chars = String.valueOf(i).toCharArray();
            for (int j = 0; j < chars.length; j++){
                int digit = Character.digit(chars[j], 10);
                raster.setPixels(x+4*j, y+yOffset, SMALL_NUMERAL_WIDTH+2, SMALL_NUMERAL_HEIGHT+2, blueDigits[digit]);
            }
        }
    }
    
    public void setMonsterListVisible(boolean visible){
        showMonsterList = true;
    }
    
    public void setSlipListVisible(boolean visible){
        showSlipList = true;
    }
    
    private static void initialiseTileGraphics(String tilespngPath) throws IOException{
        BufferedImage allTiles = ImageIO.read(new File(tilespngPath));
        for (int i = 0; i < 16 * 7; i++) {
            int x = i / 16;
            int y = i % 16;
            allTiles.getRaster().getPixels(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE, tileImage[i]);
        }
    }
    
    private static BufferedImage drawDigit(int n, Color colorBG, Color colorFG){
        int[] smallNumeralBitmap = new int[] {
            0b111_010_111_111_101_111_111_111_111_111_00,
            0b101_010_001_001_101_100_100_001_101_101_00,
            0b101_010_111_111_111_111_111_001_111_111_00,
            0b101_010_100_001_001_001_101_001_101_001_00,
            0b111_010_111_111_001_111_111_001_111_111_00
        };
        BufferedImage digit = new BufferedImage(SMALL_NUMERAL_WIDTH+2, SMALL_NUMERAL_HEIGHT+2, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = digit.createGraphics();
        graphics.setPaint(colorBG);
        graphics.fillRect(0, 0, digit.getWidth(), digit.getHeight());
        int rgb = colorFG.getRGB();
        for (int y = 0; y < SMALL_NUMERAL_HEIGHT; y++){
            for (int x = 0; x < SMALL_NUMERAL_WIDTH; x++) {
                if (((smallNumeralBitmap[y] << (x + 3*n)) & 0x80000000) == 0x80000000){
                    digit.setRGB(x+1, y+1, rgb);
                }
            }
        }
        return digit;
    }
    
    private static void initialiseDigits(){
        for (int n = 0; n <= 9; n++){
            BufferedImage digitBlue = drawDigit(n, Color.BLUE, Color.WHITE);
            digitBlue.getRaster().getPixels(0, 0, digitBlue.getWidth(), digitBlue.getHeight(), blueDigits[n]);
            BufferedImage digitBlack = drawDigit(n, Color.BLACK, Color.WHITE);
            digitBlack.getRaster().getPixels(0, 0, digitBlue.getWidth(), digitBlue.getHeight(), blackDigits[n]);
        }
    }
    
    private void initialiseBBG(){
        WritableRaster bbgRaster = bbg.getRaster();
        for (int i = 0; i < 32*32; i++){
            int x = TILE_SIZE * (i % 32), y = TILE_SIZE * (i / 32);
            bbgRaster.setPixels(x, y, TILE_SIZE, TILE_SIZE, tileImage[0]);
        }
    }

    GamePanel(SuperCC emulator, String tilespngPath) throws IOException {
        this.emulator = emulator;
        setPreferredSize(new Dimension(32*TILE_SIZE, 32*TILE_SIZE));
        addMouseListener(new GameMouseListener());
    
        initialiseTileGraphics(tilespngPath);
        initialiseDigits();
        initialiseBBG();
        
    }
    
    private class GameMouseListener implements MouseListener{
        @Override
        public void mouseClicked(MouseEvent e) {
            int x = e.getX() / TILE_SIZE;
            int y = e.getY() / TILE_SIZE;
            emulator.tick(x + (y << 5));
        }
        
        @Override
        public void mousePressed(MouseEvent e) {}
    
        @Override
        public void mouseReleased(MouseEvent e) {}
    
        @Override
        public void mouseEntered(MouseEvent e) {}
    
        @Override
        public void mouseExited(MouseEvent e) {}
    }

}
