#!/bin/bash

# 1. Load cấu hình từ file .env
if [ -f deploy.env ]; then
    source deploy.env
else
    echo "❌ Lỗi: Không tìm thấy file deploy.env!"
    exit 1
fi

echo "🚀 Bắt đầu quy trình Deploy lên Server..."

# 2. Build file JAR bằng Maven trên Mac
echo "📦 Đang build project (skip tests)..."
./gradlew build -x test

if [ $? -ne 0 ]; then
    echo "❌ Build thất bại!"
    exit 1
fi

# 3. Đẩy file JAR lên server (Ghi đè bản cũ)
echo "🚚 Đang đẩy file $JAR_NAME lên server $DEPLOY_IP..."
scp build/libs/$JAR_NAME $DEPLOY_USER@$DEPLOY_IP:$DEPLOY_PATH


if [ $? -ne 0 ]; then
    echo "❌ Lỗi khi gửi file qua SCP!"
    exit 1
fi

# 4. Khởi động lại App trong Tmux trên Server
echo "🔄 Đang restart ứng dụng trên Server..."
ssh $DEPLOY_USER@$DEPLOY_IP << EOF
    # Kiểm tra xem session tmux có tồn tại không, nếu không thì tạo mới
    if ! tmux has-session -t $TMUX_SESSION 2>/dev/null; then
        tmux new-session -d -s $TMUX_SESSION
    fi

    # Gửi lệnh tắt App (Ctrl+C), chờ 2s rồi chạy bản mới
    tmux send-keys -t $TMUX_SESSION C-c
    sleep 2
    tmux send-keys -t $TMUX_SESSION "cd $DEPLOY_PATH && java -jar $JAR_NAME" Enter
EOF

echo "✅ Deploy thành công!"