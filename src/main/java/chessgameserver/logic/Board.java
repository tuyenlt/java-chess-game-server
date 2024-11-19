package chessgameserver.logic;

import java.util.ArrayList;
import java.util.List;


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
    private List<Move> white_moves = new ArrayList<>();
    private List<Move> black_moves = new ArrayList<>();
    private List<Move> allMoves = new ArrayList<>();
    
    // Danh sách quân cờ 2 bên
    // public List<Piece> whitePieces = new ArrayList<>();
    // public List<Piece> blackPieces = new ArrayList<>();
    
    // Vị trí quân vua của 2 bên
    private int wK_row, wK_col;
    private int bK_row, bK_col;

    // Đánh dấu lượt của người chơi hiện tại
    private String currentTurn;
    
    public Board() {
        board = createBoard();
        // createPieceList();
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
    
    // Tạo danh sách các quân cờ 2 bên nếu cần
    // void createPieceList(){
    //     for(int i = 0; i < 8; i++){
    //         blackPieces.add(board[0][i].getPiece());
    //         blackPieces.add(board[1][i].getPiece());
    //         whitePieces.add(board[6][i].getPiece());
    //         whitePieces.add(board[7][i].getPiece());
    //     }
    // }


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
        board[row][col].setPiece(piece);
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
        movePiece(new Move(stringMove));
    }

    public void movePiece(Move move){
        movePiece(move, false);
    }

    public void movePiece(Move move, boolean isFakeMove){
        //Thực hiện di chuyển
        Piece piece = getPiece(move.getStartRow(), move.getStartCol());
        setPiece(move.getEndRow(), move.getEndCol(), piece);
        setPiece(move.getStartRow(), move.getStartCol(), null);
        
        //Đánh dấu trạng thái di chuyển của King và Rook
        if(piece instanceof King){
            setKing_pos(piece.getpieceColor(), move.getEndRow(), move.getEndCol());
            if(!isFakeMove)((King)piece).setHasMoved(true);
        }
        if(piece instanceof Rook){
            if(!isFakeMove)((Rook)piece).setHasMoved(true);
        }
        
        //Phép xử lý các nước đi đặc biệt
        if(move.isCastling(this)){
            Castling(move);
        }
        if(move.isPromotion(this)){
            Promotion(move.getEndRow(),move.getEndCol(),piece.getpieceColor());
        }

        //Thêm phép di chuyển vào danh sách
        if(isFakeMove) return;
        if(currentTurn.equals("w")){
            white_moves.add(move);
            currentTurn = "b";
        }else{
            black_moves.add(move);
            currentTurn = "w";
        }
        allMoves.add(move);
    }
         
    // Xử lý riêng phần nhập thành
    private void Castling (Move move){
        if(move.getEndCol() == 2){
            movePiece(new Move(move.getStartRow(),0,move.getEndRow(),3),true);
        }else{
            movePiece(new Move(move.getStartRow(),7,move.getEndRow(),5),true);
        }
    }

    // Xử lý riêng phần phong hậu
    // Phần này nếu muốn có thể nâng cấp lên thành chon 1 trong 4 quân xe, mã, tịnh, hậu.
    private void Promotion (int row, int col, String pieceColor){
        setPiece(row,col,new Queen(pieceColor));
    }

    // Kiểm tra xem quân cờ đã chọn có đang đi đi đúng lượt hay không?
    public boolean isCorrectTurn(int row, int col){
        Piece piece = getPiece(row, col);
        return piece.getpieceColor().equals(currentTurn);
    }

    //Trả về nước đi cuối cùng dạng chuỗi (phục vụ cho chế độ 1 người chơi)
    public String getLastMove(){
        return white_moves.getLast().toString();
    }

    // Kiểm tra trạng thái trò chơi ongoing , draw, win
    public String gameState(){
        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                Piece piece = getPiece(row, col);
                if(piece == null || piece.getpieceColor().equals(currentTurn)) continue;
                if(!piece.getValidMoves(this, row, col).isEmpty()) return "ongoing";
            }
        }
        if(Utils.isCheck(this, currentTurn)) return "win";
        return "draw";
    }


    public List<Move> getAllMoves(){
        return this.allMoves;
    }
}
