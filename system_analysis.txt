POCKETBUDGET KE – SYSTEM DESIGN & ANALYSIS
1. System Overview
PocketBudget KE is an offline-first Android application designed to help users monitor and understand their spending habits by automatically extracting transaction data from M-Pesa SMS messages. The system parses transaction messages, stores structured transaction records locally, categorizes spending, and generates financial summaries and insights.
________________________________________
2. Problem Statement
Most Kenyan users rely heavily on M-Pesa for daily transactions but lack an effective way to track spending patterns. Existing budgeting apps require manual logging, which is inconvenient and leads to low adoption. This causes users to lose visibility into their expenses, resulting in poor budgeting decisions and financial stress.
________________________________________
3. Proposed Solution
PocketBudget KE provides automated transaction extraction from SMS, categorization of transactions into spending groups, and generation of weekly/monthly summaries, all while ensuring privacy by storing all data locally.
________________________________________
4. Project Objectives
Main Objective
To design and develop an Android application that automatically tracks and analyzes M-Pesa spending data to improve users’ financial awareness.
Specific Objectives
•	Extract transaction details from M-Pesa SMS messages.
•	Categorize transactions into spending categories.
•	Store transaction data locally for privacy and offline access.
•	Generate spending summaries and dashboards.
•	Allow users to manually edit transaction categories and add custom categories.
________________________________________
5. System Scope
In Scope (What the system WILL do)
•	Read M-Pesa SMS messages after user permission is granted.
•	Extract key transaction information (amount, sender/receiver, date/time, transaction type).
•	Categorize transactions automatically.
•	Display dashboards (weekly/monthly).
•	Allow manual correction of categories.
•	Store data in a local database (SQLite).
•	Generate simple reports (optional export).
Out of Scope (What the system will NOT do)
•	Online payments or real money transfer.
•	Bank account integration.
•	Cloud sync.
•	Fraud detection.
•	Multi-device account login.
•	Full M-Pesa API integration.
This section is important because it protects you during presentation.
________________________________________
6. Stakeholders / Users
Primary Users
•	Students
•	Employees with frequent M-Pesa transactions
•	Small business owners
Secondary Users
•	Financial mentors (if user shares reports)
________________________________________
7. Functional Requirements (FR)
FR1: User Permission Management
•	The system shall request SMS access permission before reading messages.
•	The system shall display a privacy notice explaining local processing.
FR2: Transaction Extraction
•	The system shall scan SMS inbox for M-Pesa messages.
•	The system shall extract:
o	Amount
o	Transaction type (Sent, Received, Paybill, Buy Goods)
o	Party involved (sender/receiver/paybill name)
o	Date and time
o	Transaction ID (unique)
FR3: Transaction Storage
•	The system shall store extracted transactions in a local SQLite database.
•	The system shall prevent duplicates using transaction ID.
FR4: Transaction Categorization
•	The system shall auto-assign categories based on transaction type and keywords.
•	The system shall allow users to edit categories manually.
FR5: Dashboard and Summaries
•	The system shall display weekly and monthly spending summaries.
•	The system shall show spending breakdown per category.
•	The system shall display income vs expenses.
FR6: Search and Filter
•	The system shall allow filtering by date range.
•	The system shall allow filtering by category and transaction type.
FR7: Budget Alerts (optional but strong)
•	The system shall allow users to set a monthly budget.
•	The system shall notify users when they exceed the set budget.
________________________________________
8. Non-Functional Requirements (NFR)
NFR1: Performance
•	The system should load transactions within 3 seconds for average inbox size.
NFR2: Security & Privacy
•	The system must store all data locally.
•	The system must not upload SMS content to external servers.
NFR3: Usability
•	The system should have a simple and intuitive interface.
•	The system should minimize user effort (automation first).
NFR4: Reliability
•	The system must not crash when SMS format differs.
•	The system must ignore unreadable or unsupported messages.
NFR5: Compatibility
•	The system should support Android 8.0+ (or Android 10+ depending on permission restrictions).
________________________________________
9. System Architecture (High-Level Design)
Architecture Type
Layered Architecture (Recommended)
Presentation Layer (UI)
•	Activities/Fragments
•	Dashboard UI
•	Transaction list UI
Business Logic Layer
•	SMS parsing engine (regex)
•	Categorization logic
•	Budget computation module
Data Layer
•	SQLite database (Room optional)
•	Data Access Objects (DAO)
•	Local storage manager
External Interface
•	Android SMS Content Provider (Inbox access)
•	Notification system for alerts
________________________________________
10. System Modules
Module 1: SMS Reader Module
•	Reads messages from inbox
•	Filters only M-Pesa sender messages
•	Sends messages to parsing module
Module 2: Transaction Parser Module
•	Extracts transaction details
•	Handles different formats (send/receive/paybill/buy goods)
Module 3: Categorization Module
•	Applies keyword rules to assign categories
•	Supports manual override
Module 4: Database Module
•	Stores transactions, categories, budget settings
•	Handles updates and deletes
Module 5: Dashboard Module
•	Generates summary statistics
•	Builds graphs/charts
Module 6: User Settings Module
•	Budget settings
•	Category management
•	Privacy permission management
________________________________________
11. Data Flow Diagram (DFD Description)
Level 0 (Context Diagram)
User interacts with PocketBudget KE system to:
•	grant permissions
•	view spending dashboard
•	view categorized transactions
The system interacts with:
•	SMS Inbox (data source)
•	Local database (storage)
Level 1 DFD (Main Processes)
1.	Read SMS messages
2.	Extract transaction details
3.	Store transactions
4.	Categorize transactions
5.	Generate reports/dashboard
6.	Display output to user
________________________________________
12. Database Design (ERD Concepts)
Entities
Transaction Table
•	transactionId (PK)
•	type (Sent/Received/Paybill/BuyGoods)
•	amount
•	partyName
•	dateTime
•	categoryId (FK)
Category Table
•	categoryId (PK)
•	categoryName (Food, Transport, Airtime etc.)
Budget Table
•	budgetId (PK)
•	monthlyLimit
•	monthYear
Relationships:
•	One Category → Many Transactions
________________________________________
13. UI/UX Design (Main Screens)
Screen 1: Splash + Welcome Screen
•	App name/logo
•	brief privacy notice
Screen 2: Permission Request Screen
•	explain why SMS permission is needed
•	button: Allow / Deny
Screen 3: Dashboard Screen
•	total spent this month
•	total received this month
•	category breakdown chart
•	top spending category
Screen 4: Transactions Screen
•	list of transactions
•	filter options
Screen 5: Transaction Details Screen
•	show extracted info
•	allow category edit
Screen 6: Budget Settings Screen
•	set monthly budget
•	view progress
________________________________________
14. Testing Plan (Evaluation)
Testing Types
•	Unit Testing: parser & categorization
•	Functional Testing: permissions, extraction, dashboard
•	Usability Testing: user feedback from students
•	Performance Testing: extraction speed for large inbox
User Evaluation Method
•	5–10 test users
•	questionnaire rating:
o	ease of use
o	accuracy
o	usefulness
________________________________________
15. Assumptions and Limitations
Assumptions
•	User has M-Pesa SMS messages stored on phone.
•	User grants SMS permission.
Limitations
•	Some SMS formats may not be parsed correctly.
•	App may not work well on newer Android versions if SMS access restrictions apply.
•	Categorization is rule-based, not 100% accurate.

