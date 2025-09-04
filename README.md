# Split PRO

A secure, modern expense splitting web application built with Java Spring Boot and vanilla JavaScript.



## Features

- 🔐 **Secure Authentication** - JWT-based auth with BCrypt password hashing
- 👥 **Friend Management** - Add and track friends with balance calculations
- 📊 **Group Expenses** - Create groups and split expenses easily
- 📈 **Transaction History** - Complete audit trail with pagination
- 📤 **CSV Import/Export** - Backup and migrate your data
- 📱 **Responsive Design** - Works seamlessly on desktop and mobile
- ⚡ **Single Page Application** - Fast, smooth navigation without page reloads

## Tech Stack

**Backend:**
- Java 17
- Spring Boot 3.2.x
- Spring Security 6
- Spring Data MongoDB
- JWT (JSON Web Tokens)
- Hibernate Validator
- Maven 3.9+

**Frontend:**
- HTML5, CSS3, Vanilla JavaScript
- ES6 Modules
- History API for SPA routing
- Responsive CSS Grid/Flexbox

**Database:**
- MongoDB with automatic indexing

**Deployment:**
- Render (preferred)
- Docker support for Vercel/other platforms

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- MongoDB (local or Atlas)

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/split-pro.git
   cd split-pro
   ```

2. **Set up MongoDB**
   - Install MongoDB locally or use MongoDB Atlas
   - Default connection: `mongodb://localhost:27017/splitpro_dev`

3. **Configure environment variables** (optional)
   ```bash
   export MONGODB_URI=mongodb://localhost:27017/splitpro_dev
   export JWT_SECRET=your-secret-key-here
   export SPRING_PROFILES_ACTIVE=dev
   ```

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

5. **Access the application**
   - Open http://localhost:8080
   - The app will start with sample data for testing

### Using Maven Wrapper

The project includes Maven Wrapper, so you don't need Maven installed:

```bash
# On Unix/macOS
./mvnw spring-boot:run

# On Windows
mvnw.cmd spring-boot:run
```

## Testing

### Run Unit Tests
```bash
./mvnw test
```

### Run Integration Tests
```bash
./mvnw verify
```

### Run All Tests with Coverage
```bash
./mvnw clean test verify
```

### Run Specific Test Class
```bash
./mvnw test -Dtest=UserServiceTest
```

## Building for Production

### Create Production JAR
```bash
./mvnw clean package -Pprod
```

### Run Production Build Locally
```bash
java -jar target/split-pro-1.0.0.jar --spring.profiles.active=prod
```

## Deployment

### Option 1: Render (Recommended)

1. **Fork this repository**

2. **Connect to Render**
   - Go to [Render Dashboard](https://dashboard.render.com)
   - Connect your GitHub repository
   - Use the included `render.yaml` for automatic setup

3. **Environment Variables**
   The `render.yaml` will automatically configure:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `JWT_SECRET` (auto-generated)
   - `CORS_ALLOWED_ORIGINS`
   - `MONGODB_URI` (from managed database)

### Option 2: Docker (Vercel, etc.)

1. **Build Docker image**
   ```bash
   docker build -t split-pro .
   ```

2. **Run with Docker**
   ```bash
   docker run -p 8080:8080 \
     -e MONGODB_URI=your-mongo-uri \
     -e JWT_SECRET=your-secret \
     split-pro
   ```

3. **Deploy to container platform**
   - Use the included `Dockerfile`
   - Set required environment variables
   - Note: Some platforms may have Java/Spring Boot limitations

## Configuration

### Application Properties

The application uses YAML configuration with profile-specific overrides:

- `application.yml` - Base configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production overrides

### Key Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | Server port |
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/splitpro` | MongoDB connection |
| `app.security.jwt.secret` | `mySecretKey` | JWT signing secret |
| `app.security.jwt.access-token-expiry` | `900000` | Access token TTL (15 min) |
| `app.security.cors.allowed-origins` | `http://localhost:8080` | CORS origins |

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `MONGODB_URI` | Yes | MongoDB connection string |
| `JWT_SECRET` | Yes | Secret key for JWT signing |
| `CORS_ALLOWED_ORIGINS` | No | Comma-separated list of allowed origins |
| `SPRING_PROFILES_ACTIVE` | No | Active Spring profiles |
| `PORT` | No | Server port (for cloud platforms) |

## API Documentation

### Authentication Endpoints

- `POST /api/auth/signup` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `GET /api/auth/me` - Get current user
- `POST /api/auth/refresh` - Refresh JWT token
- `POST /api/auth/forgot-password` - Password reset request

### Core Endpoints

- `GET /api/friends` - List friends with balances
- `POST /api/groups` - Create new group
- `GET /api/groups/{id}/balances` - Get group balances
- `POST /api/expenses` - Add new expense
- `GET /api/history` - Transaction history (paginated)
- `POST /api/settlements` - Record settlement
- `POST /api/csv/export` - Export data to CSV
- `POST /api/csv/import` - Import data from CSV

## Security Features

- **Password Security**: BCrypt hashing with configurable strength
- **JWT Authentication**: HTTP-only cookies with refresh token rotation
- **CSRF Protection**: CSRF tokens for form submissions
- **Input Validation**: Hibernate Validator with custom constraints
- **Rate Limiting**: Authentication endpoint rate limiting
- **CORS Protection**: Configurable allowed origins
- **Security Headers**: HSTS, X-Frame-Options, X-Content-Type-Options
- **SQL Injection Prevention**: MongoDB's built-in protection
- **XSS Prevention**: Content Security Policy and input sanitization

## Architecture

### Project Structure
```
src/main/java/com/splitpro/
├── SplitProApplication.java          # Main application class
├── config/                           # Configuration classes
│   ├── SecurityConfig.java           # Spring Security setup
│   ├── MongoConfig.java             # MongoDB configuration
│   └── CorsConfig.java              # CORS configuration
├── controller/                       # REST controllers
├── service/                          # Business logic
├── repository/                       # Data access layer
├── model/                           # Entity classes
├── dto/                             # Data transfer objects
└── exception/                       # Exception handling

src/main/resources/
├── application.yml                   # Configuration files
├── application-dev.yml
├── application-prod.yml
└── static/                          # Frontend assets
    ├── index.html
    ├── css/styles.css
    └── js/app.js

src/test/java/                       # Test classes
```

### Data Model

**User**: Stores user information and authentication data
**Group**: Represents expense groups with member lists
**Expense**: Individual expense records with split details
**Settlement**: Payment records between users

## Code Quality

### Formatting
```bash
# Format code
./mvnw spotless:apply

# Check formatting
./mvnw spotless:check
```

### Static Analysis
The project includes:
- Spotless for code formatting
- Maven Enforcer for dependency management
- Spring Boot's built-in security scanning

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run the test suite
6. Submit a pull request

## Troubleshooting

### Common Issues

**MongoDB Connection Failed**
- Check if MongoDB is running
- Verify connection string format
- Ensure database user has proper permissions

**JWT Token Issues**
- Verify `JWT_SECRET` is set and consistent
- Check token expiration settings
- Clear browser cookies if needed

**Build Failures**
- Ensure Java 17+ is being used
- Clear Maven cache: `./mvnw clean`
- Check for dependency conflicts

**Port Already in Use**
- Change port: `./mvnw spring-boot:run -Dserver.port=8081`
- Kill existing process using the port

### Getting Help

- Check the application logs for detailed error messages
- Review the test cases for usage examples
- Consult Spring Boot documentation for configuration help

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, please use the in-app support form or create an issue in the GitHub repository.
