<?php
declare(strict_types=1);

class UserNotFoundException extends Exception {
    public function __construct(string $id = "") {
        parent::__construct("User with ID $id not found!");
    }
}

class Logger {
    public function log(string $message): void {
        file_put_contents('error.log', $message . PHP_EOL, FILE_APPEND);
    }
}

const USERS = [
    "1" => "Julien",
    "2" => "Rajerison",
    "3" => "Jul",
];

class UserRepository {
    public function findUser(string $id): string {
        if (!isset(USERS[$id])) {
            throw new UserNotFoundException($id);
        }
        return USERS[$id];
    }
}

class UserService {
    private UserRepository $repository;
    private Logger $logger;

    public function __construct() {
        $this->repository = new UserRepository();
        $this->logger = new Logger();
    }

    public function getUserById(string $id): ?string {
        try {
            return $this->repository->findUser($id);
        } catch (UserNotFoundException $e) {
            $this->logger->log("Service: " . $e->getMessage());
            return null;
        } catch (Exception $e) {
            $this->logger->log("Service (Unknown error): " . $e->getMessage());
            return null;
        }
    }
}

class Controller {
    private UserService $service;
    private Logger $logger;

    public function __construct() {
        $this->service = new UserService();
        $this->logger = new Logger();
    }

    public function getCurrentUser(string $id): string {
        try {
            $user = $this->service->getUserById($id);
            if ($user === null) {
                throw new UserNotFoundException($id);
            }
            return $user;
        } catch (UserNotFoundException $e) {
            $this->logger->log("Controller: " . $e->getMessage());
            return "User not found!";
        } catch (Exception $e) {
            $this->logger->log("Controller: Internal server error - " . $e->getMessage());
            return "Une erreur est survenue !";
        }
    }
}

$main = new Controller();
echo $main->getCurrentUser("7");
