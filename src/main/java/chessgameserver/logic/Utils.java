package chessgameserver.logic;

import java.util.HashMap;
import java.util.LinkedHashSet;

public class Utils {

    // Kiểm tra trạng thái của ô
    static boolean isEmpty(Board board, int row, int col) {
        if(row > 7 || row < 0 || col > 7 || col < 0){
            return false;
        }
        return board.isEmpty(row, col);
    }

    // Kiểm tra điều kiện di chuyển của quân cờ
    static boolean isMoveAllowed(Board board, String pieceColor, int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) return false;
        if (isEmpty(board, row, col)) return true;
        return !board.getPiece(row, col).getpieceColor().equals(pieceColor);
    }

    // Kiểm tra xem ô (row, col) có đang bị tấn công không
    static boolean isUnderAttack(Board board, String pieceColor, int targetRow, int targetCol) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece == null || piece.getpieceColor().equals(pieceColor)) continue;
                if (piece.canAtack(board, row, col, targetRow, targetCol)) return true;
            }
        }
        return false;
    }

    // Kiểm tra trạng thái bị chiếu của King
    static boolean isCheck(Board board, String pieceColor) {
        int kingRow = board.getKingRow(pieceColor);
        int kingCol = board.getKingCol(pieceColor);
        return isUnderAttack(board, pieceColor, kingRow, kingCol);
    }

    // Kiểm tra nếu một nước đi là an toàn cho quân cờ
    static boolean isSafeMove(Board board, String pieceColor, Move move) {
        if(move.isEnPassant()) return true;
        Piece originalPiece = board.getPiece(move.getStartRow(), move.getStartCol());
        Piece targetPiece = board.getPiece(move.getEndRow(), move.getEndCol());

        // Di chuyển giả
        board.movePiece(move, true);
        boolean safe = !isCheck(board, pieceColor);

        // Khôi phục lại
        board.movePiece(move.getReverseMove(), true);
        board.setPiece(move.getStartRow(), move.getStartCol(), originalPiece);
        board.setPiece(move.getEndRow(), move.getEndCol(), targetPiece);

        if (move.isPromotion(board)) {
            board.setPiece(move.getStartRow(), move.getStartCol(), new Pawn(pieceColor));
        }

        return safe;
    }

    // Kiểm tra điều kiện nhập thành trái
    static boolean canCastleLeft(Board board, int startRow, int startCol, boolean hasMoved, String pieceColor) {
        if (hasMoved || isCheck(board, pieceColor)) return false;
        int initialRow = (pieceColor.equals("w") ? 7 : 0);
        Piece piece = board.getPiece(initialRow, 0);    
        if (!(piece instanceof Rook) || !piece.getpieceColor().equals(pieceColor)) return false;
        Rook rook = (Rook) piece;
        if (rook.isMove()) return false;
        for (int col = 1; col < 4; col++) {
            if (!isEmpty(board, initialRow, col) || isUnderAttack(board, pieceColor, initialRow, col)) {
                return false;
            }
        }
        return true;
    }

    // Kiểm tra điều kiện nhập thành phải
    static boolean canCastleRight(Board board, int startRow, int startCol, boolean hasMoved, String pieceColor) {
        int initialRow = (pieceColor.equals("w") ? 7 : 0);
        if (hasMoved || isCheck(board, pieceColor)) return false;
        Piece piece = board.getPiece(initialRow, 7);
        if (!(piece instanceof Rook) || !piece.getpieceColor().equals(pieceColor)) return false;
        Rook rook = (Rook) piece;
        if (rook.isMove()) return false;
        for (int col = 5; col < 7; col++) {
            if (!isEmpty(board, initialRow, col) || isUnderAttack(board, pieceColor, initialRow, col)) {
                return false;
            }
        }
        return true;
    }


    static Move canEnPassant(Board board, String color, int startRow, int startCol){
        if (startRow != ((color.equals("w")) ? 3 : 4)) return null;
        Move move = board.getLastMove();
        if(move ==  null){
            return null;
        }
        int endRow = move.getEndRow();
        int endCol = move.getEndCol();
        if (!(board.getPiece(endRow,endCol) instanceof Pawn)) return null;
        if (endRow != startRow || Math.abs(endCol - startCol)!=1) return null;
        Move enPassantMove = new Move(startRow, startCol,((startRow == 3) ? 2:5) , endCol);
        enPassantMove.setEnPassant(true);
        return enPassantMove;
    }

    // Trạng thái không có nước đi hợp lệ nhưng vua không bị chiếu
    static boolean isStaleMate(Board board, String pieceColor){
        if(isCheck(board, pieceColor)) return false;
        for(int row = 0; row < 8; row ++){
            for(int col = 0 ;col <8; col++){
                Piece piece = board.getPiece(row, col);
                if(piece == null || !piece.getpieceColor().equals(pieceColor)) continue;
                if(!piece.getSafeMoves(board, row, col).isEmpty()) return false; 
            }
        }
        return true;
    }

    // Hòa do lực lượng không đủ để chiếu hết
    static boolean isInsufficientMaterial(HashMap<Character,Integer> whitePieces, HashMap<Character,Integer> blackPieces){
        // Có quân tốt, hậu, xe thì không thể hòa
        if (whitePieces.get('P') > 0 || blackPieces.get('P')>0) return false;
        if (whitePieces.get('R') > 0 || blackPieces.get('R')>0) return false;
        if (whitePieces.get('Q') > 0 || blackPieces.get('Q')>0) return false;
        
        // Trường hợp còn 2 con mã và 1 vua đối đầu với 1 vua thì chắc chắn sẽ hòa
        if (whitePieces.get('N') ==2 && (blackPieces.get('B') + blackPieces.get('N'))==0) return true;
        if (blackPieces.get('N') ==2 && (whitePieces.get('B') + whitePieces.get('N'))==0) return true;

        // Nếu quân trắng (hoặc đen) có nhiều hơn 1 con tượng và mã thì không thể hòa
        if ((whitePieces.get('B') + whitePieces.get('N')) >1) return false;
        if ((blackPieces.get('B') + blackPieces.get('N')) >1) return false;

        // Những trường hợp còn lại thì chắc chắn hòa
        return true;
    }

    // Hòa do lặp lại thế cờ ba lần
    static boolean isThreefoldRepetition(LinkedHashSet<String> boardPositions, int steps){
        // Luật quốc tế lằng nhằng khó nhai lắm, chắc phải chục dòng if:))
        // Thôi thì mình để tạm là lặp lại vị trí các quân cờ >= 3 lần
        return steps - boardPositions.size() >= 3;
    }
}
