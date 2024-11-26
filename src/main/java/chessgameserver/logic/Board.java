package chessgameserver.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashSet;


class Box {
    private Piece piece;

    Box(Piece piece) {
        this.piece = piece;
    }

    Box() {
        this.piece = null;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public Piece getPiece() { 
        return piece;
    }
}

public class Board {
    private Box[][] board;
    
    // Thêm danh sách lịch sử di chuyển của 2 bên để tiện sử dụng sau này
    private List<String> whiteMoves = new ArrayList<>();
    private List<String> blackMoves =new ArrayList<>();
    private List<String> allMoves = new ArrayList<>();

    // Danh sách thế cờ(theo dạng String) sau mỗi nước đi
    private LinkedHashSet<String> boardPositions = new LinkedHashSet<>();
    
    //Danh sách số lượng quân cờ 2 bên
    HashMap<Character,Integer> whitePieces;
    HashMap<Character,Integer> blackPieces;
    
    // Vị trí quân vua của 2 bên
    private int wK_row, wK_col;
    private int bK_row, bK_col;

    // Đánh dấu lượt của người chơi hiện tại
    private String currentTurn;
    private boolean isBoardReverse = false;
    
    public Board() {
        board = createBoard();
        whitePieces = createPieceList();
        blackPieces = createPieceList();
        wK_row = 7;
        wK_col = 4;
        bK_row = 0;
        bK_col = 4;
        currentTurn ="w";
    }

    private Box[][] createBoard() {
        Box[][] initialBoard = new Box[8][8];

        // Khởi tạo hàng 1 với quân đen
        initialBoard[0][0] = new Box(new Rook("b"));
        initialBoard[0][1] = new Box(new Knight("b"));
        initialBoard[0][2] = new Box(new Bishop("b"));
        initialBoard[0][3] = new Box(new Queen("b"));
        initialBoard[0][4] = new Box(new King("b"));
        initialBoard[0][5] = new Box(new Bishop("b"));
        initialBoard[0][6] = new Box(new Knight("b"));
        initialBoard[0][7] = new Box(new Rook("b"));

        for (int i = 0; i < 8; i++) {
            initialBoard[1][i] = new Box(new Pawn("b"));
        }

        for (int i = 2; i < 6; i++) {
            for (int j = 0; j < 8; j++) {
                initialBoard[i][j] = new Box();
            }
        }

        for (int i = 0; i < 8; i++) {
            initialBoard[6][i] = new Box(new Pawn("w"));
        }

        initialBoard[7][0] = new Box(new Rook("w"));
        initialBoard[7][1] = new Box(new Knight("w"));
        initialBoard[7][2] = new Box(new Bishop("w"));
        initialBoard[7][3] = new Box(new Queen("w"));
        initialBoard[7][4] = new Box(new King("w"));
        initialBoard[7][5] = new Box(new Bishop("w"));
        initialBoard[7][6] = new Box(new Knight("w"));
        initialBoard[7][7] = new Box(new Rook("w"));

        return initialBoard;
    }

    public void setReverse(Boolean isBoardReverse){
        this.isBoardReverse = isBoardReverse;
    }

    public String[][] getBoardState(){
        String[][] broadState = new String[8][8];
        for(int i=0; i<8; i++){
            for(int j=0; j<8; j++){
                if(board[i][j].getPiece() != null){
                    broadState[i][j] = board[i][j].getPiece().getName();
                }else{
                    broadState[i][j] = " ";
                }
            }
        }
        return broadState;
    }
    
    // Tạo danh sách các quân cờ 2 bên nếu cần
    HashMap<Character,Integer> createPieceList(){
        HashMap<Character,Integer> pieces = new HashMap<>();
        pieces.put('P', 8);
        pieces.put('R', 2);
        pieces.put('N', 2);
        pieces.put('B', 2);
        pieces.put('Q', 1);
        pieces.put('K', 1);
        return pieces; 
    }


    // Kiểm tra box ở pos có trống không
    public boolean isEmpty(int row, int col){
        return getPiece(row, col)==null;
    }

    // Lấy thông tin quân cờ
    public Piece getPiece(int row, int col){
        return board[row][col].getPiece();
    }

    // Thiết lập quân cờ
    public void setPiece(int row, int col, Piece piece){
        if (!isEmpty(row, col)){
            popPiece(getPiece(row, col).getName());
        }
        board[row][col].setPiece(piece);
        if (piece != null){
            addPiece(piece.getName());
        }
    }

    // Lấy thông tin vị trí của King
    public int getKingRow(String pieceColor){
        if(pieceColor.equals("w")) return wK_row;
        else return bK_row;
    }
 
    public int getKingCol(String pieceColor){
        if(pieceColor.equals("w")) return wK_col;
        else return bK_col;
    }

    // Thiết lập vị trí của King
    public void setKing_pos(String pieceColor, int row, int col){
        if(pieceColor.equals("w")){
            wK_row=row;
            wK_col=col;
        }else{
            bK_row=row;
            bK_col=col;
        }  
    }

    // Trả về lượt người chơi hiện tại
    public String getCurrentTurn(){
        return currentTurn;
    }

    // Hàm kiểm tra xem 1 nước đi có hợp lệ hay không
    public boolean isValidMove(int startRow, int startCol, int endRow,int endCol){
        Piece piece = getPiece(startRow, startCol);
        Move move = new Move(startRow, startCol, endRow, endCol);
        return piece.getSafeMoves(this, startRow, startCol).contains(move); 
    }

    

    // Thực hiện di chuyển quân cờ
    public void movePiece(int startRow, int startCol, int endRow,int endCol){
        movePiece(new Move(startRow, startCol, endRow, endCol));
    }

    // Di chuyển quân cờ theo dạng chuỗi
    public void movePiece(String stringMove){
        // System.out.println(stringMove);
        movePiece(new Move(stringMove));
        allMoves.add(stringMove);                               
    }

    public void movePiece(Move move){
        movePiece(move, false);
    }

    public void movePiece(Move move, boolean isFakeMove){
        //Thực hiện di chuyển
        Piece piece = getPiece(move.getStartRow(), move.getStartCol());
        if(move.isCastling(this)){
            Castling(move);
        }
        setPiece(move.getEndRow(), move.getEndCol(), piece);
        if(move.isPromotion(this)){
            Promotion(move.getEndRow(),move.getEndCol(),piece.getpieceColor(), move.getPromotedPieceType());
        }
        setPiece(move.getStartRow(), move.getStartCol(), null);
        
        //Đánh dấu trạng thái di chuyển của King và Rook
        if(piece instanceof King){
            setKing_pos(piece.getpieceColor(), move.getEndRow(), move.getEndCol());
            if(!isFakeMove)((King)piece).setHasMoved(true);
        }
        if(piece instanceof Rook){
            if(!isFakeMove)((Rook)piece).setHasMoved(true);
        }
        
        //Thêm phép di chuyển vào danh sách
        
        // Tốt qua đường
        if(move.isEnPassant()){
            System.out.println("pass");
            setPiece(move.getStartRow(), move.getEndCol(),null);
        }
        
        if(isFakeMove) return;

        if(currentTurn.equals("w")){
            whiteMoves.add(move.toString());
            currentTurn = "b";
        }else{
            blackMoves.add(move.toString());
            currentTurn = "w";
        }
        if(!move.isTurnBot())allMoves.add(move.toString());

        // Thêm thế cờ mới
        boardPositions.add(toString());
        System.out.println(this);
    }
         
    // Xử lý riêng phần nhập thành
    private void Castling (Move move){
        System.out.println("casle");
        if(move.getEndCol() == 2){
            movePiece(new Move(move.getStartRow(),0,move.getEndRow(),3),true);
        }else{
            movePiece(new Move(move.getStartRow(),7,move.getEndRow(),5),true);
        }
    }

    // Xử lý riêng phần phong hậu
    // Phần này nếu muốn có thể nâng cấp lên thành chon 1 trong 4 quân xe, mã, tịnh, hậu.
    private void Promotion (int row, int col, String pieceColor, String pieceType){
        if(pieceType.equals("")){
            return;
        }
        System.out.println("promotion call:" + pieceColor + " " + pieceType);
        if (pieceType.equals("q")){
            System.out.println("promotion to queen");
            setPiece(row,col,new Queen(pieceColor));
        }else if(pieceType.equals("r")){
            setPiece(row, col, new Rook(pieceColor));
        }else if(pieceType.equals("b")){
            setPiece(row, col, new Bishop(pieceColor));
        }else {
            setPiece(row, col, new Knight(pieceColor));
        }
    }

    // Kiểm tra xem quân cờ đã chọn có đang đi đi đúng lượt hay không?
    public boolean isCorrectTurn(int row, int col){
        if(isBoardReverse){
            row = 7 - row;
            col = 7 - col;
        }
        Piece piece = getPiece(row, col);
        if(piece == null){
            return false;
        }
        return piece.getpieceColor().equals(currentTurn);
    }

    //Trả về nước đi cuối cùng dạng chuỗi (phục vụ cho chế độ 1 người chơi)
    public String getLastMoveString(){
        return whiteMoves.getLast().toString();
    }

    public Move getLastMove(){
        if(allMoves.isEmpty()){
            return null;
        }
        return new Move(allMoves.getLast());
    }

    public List<String> getMoves(String side){
        if(side.equals("b")){
            return this.blackMoves;
        }
        if(side.equals("w")){
            return this.whiteMoves;
        }
        return this.allMoves;
    }

    // Kiểm tra trạng thái bị chiếu bí của người chơi hiện tại
    public boolean isCheckMate(){
        if(!Utils.isCheck(this, currentTurn)) return false;
        for(int row = 0; row < 8; row ++){
            for(int col = 0 ;col <8; col++){
                Piece piece = getPiece(row, col);
                if(piece == null || !piece.getpieceColor().equals(currentTurn)) continue;
                if(!piece.getSafeMoves(this, row, col).isEmpty()) return false; 
            }
        }
        System.out.println(((currentTurn.equals("w")) ? "Black" : "White") +" Player win!");
        return true;
    } 

    // Kiểm tra trạng thái hòa
    public boolean isDraw(){
        if (Utils.isStaleMate(this, currentTurn)) return true;
        if (Utils.isInsufficientMaterial(whitePieces,blackPieces)) return true;
        return false;
    }

    // Trả về kết quả của ván đấu
    public String gameState(){
        if(isCheckMate()) return "win";
        if(isDraw()) return "draw";
        return "ongoing";
    }

    private void popPiece(String namePiece){
        char pieceType = namePiece.charAt(1);
        if(namePiece.charAt(0) == 'w'){
            whitePieces.put(pieceType, whitePieces.get(pieceType)-1);
        }else{
            blackPieces.put(pieceType, whitePieces.get(pieceType)-1);
        }
    }

    private void addPiece(String namePiece){
        char pieceType = namePiece.charAt(1);
        if(namePiece.charAt(0) == 'w'){
            whitePieces.put(pieceType, whitePieces.get(pieceType)+1);
        }else{
            blackPieces.put(pieceType, whitePieces.get(pieceType)+1);
        }
    }

    @Override
    public String toString() {
        String output = "------------------------" +'\n';
        for(int row = 0; row <8; row++ ){
            String line ="";
            for(int col = 0 ; col<8; col++){
                Piece piece = getPiece(row, col);
                if (piece !=null) line += (getPiece(row, col).getName() +" ");
                else line +=("-- ");
            }
            output += (line +'\n');
        }
        output += "------------------------";
        return output;
    }
}
