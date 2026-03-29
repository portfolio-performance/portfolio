```mermaid
classDiagram
    direction TB
    class Client {
    		-List~Security~ securities
    		-List~Watchlist~ watchlists
    		-List~Account~ accounts
    		-List~Portfolio~ portfolios
    		-List~InvestmentPlan~ plans
    		-List~Taxonomy~ taxonomies
    		-List~Dashboard~ dashboards
    }
    class Account {
    		-String name
    		-List~AccountTransaction~ transactions
    }
    class Portfolio {
        -String name
        -List~PortfolioTransaction~ transactions
    }
    class Security {
        -String name
        -String isin
        -String wkn
    }
    class Watchlist {
    		-List~Security~ securities
    }
    class Dashboard {
    }
    class InvestmentPlan {
    }
    class Taxonomy {
    }
    namespace Transactions {
	    class Transaction {
    			-LocalDateTime date
    			-Security security
    		}    
	    class AccountTransaction {
    		}
	    class PortfolioTransaction {
    		}
    }
    
    Client "1" o-- "*" Dashboard
    Client "1" o-- "*" InvestmentPlan
    Client "1" o-- "*" Account
    Client "1" o-- "*" Portfolio
    Client "1" o-- "*" Security
    Client "1" o-- "*" Taxonomy
    Client "1" o-- "*" Watchlist
    Account "1" o-- "*" AccountTransaction
    Portfolio "1" o-- "*" PortfolioTransaction
    Transaction <|-- AccountTransaction
    Transaction <|-- PortfolioTransaction
    Transaction "*" -- "0..1" Security
    Watchlist "1" o-- "*" Security

