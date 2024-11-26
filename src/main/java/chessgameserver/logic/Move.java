package chessgameserver.logic;

public class Move {
    private int startRow, startCol;
    private int endRow, endCol;
    private boolean isTurnBot = false;
    private String promotedPieceType = "";
    private boolean isEnPassant = false;

    

    

    // Khởi tạo nước đi từ chuỗi
    public Move(String moveString) {
        this.startCol = moveString.charAt(0) - 'a'; 
        this.startRow = 8 - (moveString.charAt(1) - '0');
        this.endCol = moveString.charAt(2) - 'a';
        this.endRow = 8 - (moveString.charAt(3) - '0');
        this.isTurnBot = true;
        if(moveString.length()>4){
            promotedPieceType = String.valueOf(moveString.charAt(4));
        }
    }

    public Move(int startRow, int startCol, int endRow, int endCol) {
        this.startRow = startRow;
        this.startCol = startCol;
        this.endRow = endRow;
        this.endCol = endCol;
    }

    // Khởi tạo nước đi với quân được phong
    public Move(int startRow, int startCol, int endRow, int endCol, String promotedPieceType ) {
        this(startRow, startCol, endRow, endCol);
        this.promotedPieceType = promotedPieceType;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    public int getEndRow() {
        return endRow;
    }

    public int getEndCol() {
        return endCol;
    }

    public String getPromotedPieceType(){
        return promotedPieceType;
    }

    public boolean isTurnBot(){
        return isTurnBot;
    }

    public boolean isEnPassant() {
        return isEnPassant;
    }
    // Kiểm tra nước đi có phải nhập thành hoặc phong hậu không?
    public boolean isCastling(Board board) {
        Piece piece = board.getPiece(startRow, startCol);
        return piece instanceof King && Math.abs(endCol - startCol) == 2;
    }

    public boolean isPromotion(Board board) {
        Piece piece = board.getPiece(startRow, startCol);
        return piece instanceof Pawn && (endRow == 0 || endRow == 7);
    }

    public void setPromotedPieceType(String promotedPieceType){
        this.promotedPieceType = promotedPieceType; 
    }

    public void setEnPassant(boolean isEnPassant) {
        this.isEnPassant = isEnPassant;
    }

    // Lấy nước đi ngược lại
    public Move getReverseMove() {
        return new Move(endRow, endCol, startRow, startCol);
    }

    public void reverseBoard(){ // bàn cờ người chơi quân đen online sẽ ngược lại;
        startCol = 7 - startCol;
        startRow = 7 - startRow;
        endCol = 7 - endCol;
        endRow = 7 - endRow;
    }

    // toString để in nước đi dạng chuẩn
    @Override 
    public String toString() { 
        char startColChar = (char) ('a' + startCol);
        char endColChar = (char) ('a' + endCol);
        int startRowNum = 8 - startRow;
        int endRowNum = 8 - endRow;
        return "" + startColChar + startRowNum + endColChar + endRowNum + promotedPieceType;
    }
    @Override
    public boolean equals(Object obj){
        Move move = (Move) obj;
        return startRow == move.startRow && startCol == move.startCol 
            && endRow == move.endRow && endCol == move.endCol;
    }
}
