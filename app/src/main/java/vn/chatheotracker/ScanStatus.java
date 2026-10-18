package vn.chatheotracker;

/** Short on-screen labels. Preserve numeric detail but avoid clipped sentences. */
final class ScanStatus {
    static String compact(String raw) {
        if(raw==null || raw.trim().isEmpty())return "chờ";
        String s=raw.trim();
        if(s.matches("\\d+ lá \u00b7 chưa đối soát"))return s.replace("chưa đối soát","lệch");
        if(s.matches("\\d+ lá \u00b7 thiếu lịch sử \\d+"))
            return s.replace("thiếu lịch sử ","thiếu ");
        if(s.contains("thiếu bài / lịch sử"))return "lệch bài/LS";
        if(s.contains("mở game / kiểm tra ghi màn hình"))return "mở game / quyền";
        if(s.contains("chờ game · thử kết nối lại"))return "chờ game / nối lại";
        if(s.contains("đang xử lý"))return "đang tính";
        if(s.contains("đã nhận hình"))return "có hình";
        if(s.contains("lỗi khung hình"))return "lỗi hình";
        if(s.contains("mất khung hình"))return "mất hình";
        if(s.contains("reset"))return "đã reset";
        if(s.contains("quét lại"))return "quét lại";
        if(s.length()>23)return s.substring(0,21)+"…";
        return s;
    }
    private ScanStatus(){}
}
