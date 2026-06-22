# 2. Quản lý Phim (Movies)

## Dành cho Admin (Yêu cầu ROLE_ADMIN)

- **Tạo phim mới:** `POST /api/v1/admin/movies/create`
    
- **Cập nhật phim:** `PUT /api/v1/admin/movies/update/{id}`
    
- **Xóa phim:** `DELETE /api/v1/admin/movies/delete/{id}`
    
- **Request Body common (UpdateOrCreateMovieDTO):**
    
    - `title` (string)
        
    - `description` (string)
        
    - `duration` (integer, **min: 15**)
        
    - `releaseDate` (string, format: date)
        
    - `ageRating` (string)
        
    - `posterUrl` (string)
        
    - `trailerUrl` (string)
        
    - `directorName` (string)
        
    - `actorList` (string)
        
    - `genreIdList` (array of int64, **required**)
        
- **Response:** `ApiResponseMovieDetailDTO` chứa thông tin phim và `genres` (mảng GenreDTO).
    

## Dành cho Public (Không yêu cầu đăng nhập)

- **Tìm kiếm/Danh sách phim:** `GET /api/v1/public/movies`
    
    - Query: `title` (string), `genreId` (int64), `ageRating` (string), `pageable` (Pageable object)
    - Mặc định: `size=10, sort="releaseDate,desc"`
        
- **Phim nổi bật (Now showing & Upcoming):** `GET /api/v1/public/movies/featured`
    
    - Trả về 5 phim đang chiếu (releaseDate <= today) và 5 phim sắp chiếu (releaseDate > today).

```json
{
  "code": 200,
  "data": {
    "nowShowing": [
      {
        "movieId": 1,
        "title": "Michael",
        "duration": 130,
        "releaseDate": "2026-04-24",
        "ageRating": "T13",
        "posterUrl": "https://..."
      }
    ],
    "upcoming": [
      {
        "movieId": 3,
        "title": "The Mandalorian and Grogu",
        "duration": 120,
        "releaseDate": "2026-05-22",
        "ageRating": "T13",
        "posterUrl": "https://..."
      }
    ]
  }
}
```

- **Chi tiết phim:** `GET /api/v1/public/movies/{id}`

```json
{
  "code": 200,
  "data": {
    "movieId": 1,
    "title": "Michael",
    "description": "Chân dung điện ảnh về Michael Jackson...",
    "duration": 130,
    "releaseDate": "2026-04-24",
    "ageRating": "T13",
    "posterUrl": "https://...",
    "trailerUrl": "https://www.youtube.com/watch?v=...",
    "directorName": "Antoine Fuqua",
    "actorList": "Jaafar Jackson, Colman Domingo, Nia Long, Miles Teller",
    "genres": [
      { "genreId": 6, "name": "Drama" },
      { "genreId": 10, "name": "Musical" }
    ]
  }
}
```
    
- **Danh sách thể loại:** `GET /api/v1/public/genres/all`
