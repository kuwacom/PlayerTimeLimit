# 📖 PlayerTimeLimit

PlayerTimeLimitは、Minecraftサーバー上でプレイヤーのプレイ時間を制限するプラグインです  
このプラグインは、[Ajneb97/PlayerTimeLimit](https://github.com/Ajneb97/PlayerTimeLimit/)のフォークで、新規プロジェクトとしてintelliJ-IDE環境でMinecraftPluginを利用してGradleで再構成を行い、最新バージョンに対応させています

## ✨ 特徴

- ⏰ **プレイ時間の制限**: プレイヤーの1日のプレイ時間を設定し、制限を超えるとキックします
- 👑 **権限別制限**: VIPやOPなどの権限に応じて異なる時間制限を設定できます
- 🌍 **ワールドホワイトリスト**: 特定のワールドでのみ時間をカウントするシステム
- 📢 **通知機能**: 残り時間が少ない場合にアクションバーやボスバーで通知
- 🔄 **自動リセット**: 指定時間にプレイ時間をリセット
- 📊 **データ保存**: プレイヤーのデータを定期的に保存
- 🔧 **API対応**: PlaceholderAPIとの連携で、HUDなどに時間を表示可能

## 📦 インストール

1. [リリースページ](https://github.com/kuwacom/PlayerTimeLimit/releases)から最新のJARファイルをダウンロードします
2. サーバーの`plugins`フォルダにJARファイルを配置します
3. サーバーを再起動または`/reload`コマンドを実行します
4. 設定ファイルを編集してカスタマイズします

### 依存関係

- **Multiverse-Core** (オプション): マルチワールド対応
- **PlaceholderAPI** (オプション): プレースホルダー機能を使用する場合

## ⚙️ 設定

`config.yml`ファイルを編集してプラグインをカスタマイズします。

### 主な設定項目

- `time_limits`: 権限別の時間制限（秒単位）
  - `default`: デフォルトの制限時間（例: 3600秒 = 1時間）
  - `vip`: VIPの制限時間
  - `op`: OPの制限時間（0で無制限）
- `world_whitelist_system`: ワールドホワイトリストの有効化と対象ワールド
- `teleport_coordinates_on_kick`: キック時のテレポート座標
- `reset_time`: プレイ時間をリセットする時間（例: "00:00"）
- `notification`: 残り時間に応じた通知メッセージ
- `boss_bar` / `action_bar`: 通知方法の設定

### 例

```yaml
time_limits:
  default: 3600
  vip: 7200
  op: 0
```

## 🛠️ コマンド

| コマンド | 説明 | 使用例 |
|----------|------|--------|
| `/ptl` | メインコマンド | `/ptl` |
| `/ptl reload` | 設定をリロード | `/ptl reload` |
| `/ptl info` | リセット時間と残り時間を表示 | `/ptl info` |
| `/ptl check <player>` | プレイヤーの時間を確認 | `/ptl check Steve` |
| `/ptl addtime <player> <time>` | プレイヤーに時間を追加 | `/ptl addtime Steve 3600` |
| `/ptl taketime <player> <time>` | プレイヤーから時間を削除 | `/ptl taketime Steve 1800` |
| `/ptl resettime <player>` | プレイヤーの時間をリセット | `/ptl resettime Steve` |

## 🔐 パーミッション

- `playertimelimit.reload`: 設定リロード権限
- `playertimelimit.check`: プレイヤー時間確認権限
- `playertimelimit.addtime`: 時間追加権限
- `playertimelimit.taketime`: 時間削除権限
- `playertimelimit.resettime`: 時間リセット権限
- `playertimelimit.bypass`: 時間制限を無視する権限

## 🚨 サポート

- 📧 **issu**: [GitHub Issues](https://github.com/kuwacom/PlayerTimeLimit/issues)でバグ報告や機能リクエストをお願いします

## 📜 ライセンス

このプラグインはオープンソースです  
詳細は[ライセンスファイル](LICENSE)を参照してください

## フォーク元
https://github.com/Ajneb97/PlayerTimeLimit/
https://www.spigotmc.org/resources/96577/
