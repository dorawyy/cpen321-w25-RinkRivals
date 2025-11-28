# Testing and Code Review

## 1. Change History

| **Change Date** | **Modified Sections** | **Rationale**                         |
| --------------- | --------------------- | ------------------------------------- |
| 26. Nov.        | 2.1.1                 | added some missing Tests to the table |
|                 |                       |                                       |

---

## 2. Back-end Test Specification: APIs

### 2.1. Locations of Back-end Tests and Instructions to Run Them

#### 2.1.1. Tests

| **Interface**                       | **Describe Group Location, No Mocks**                        | **Describe Group Location, With Mocks**                   | **Mocked Components** |
| ----------------------------------- | ------------------------------------------------------------ | --------------------------------------------------------- | --------------------- |
| **GET /api/tickets/user/:userId**   | `tests/unmocked/bingo-tickets/get-user-tickets.test.ts`      | `tests/mocked/bingo-tickets/get-user-tickets-M.test.ts`   | Ticket DB, User DB    |
| **GET /api/tickets/:id**            | `tests/unmocked/bingo-tickets/get-ticket-by-id.test.ts`      | `tests/mocked/bingo-tickets/get-ticket-by-id-M.test.ts`   | Ticket DB, User DB    |
| **POST /api/tickets**               | `tests/unmocked/bingo-tickets/post-bingo-tickets-NM.test.ts` | `tests/mocked/bingo-tickets/post-bingo-tickets-M.test.ts` | Ticket DB, User DB    |
| **PUT /api/tickets/:id/crossedOff** | `tests/unmocked/bingo-tickets/update-crossed-off.test.ts`    | `tests/mocked/bingo-tickets/update-crossed-off-M.test.ts` | Ticket DB, User DB    |
| **DELETE /api/tickets/:id**         | `tests/unmocked/bingo-tickets/delete-ticket.test.ts`         | `tests/mocked/bingo-tickets/delete-ticket-M.test.ts`      | Ticket DB, User DB    |
| **POST /api/friends/send**          | `tests/unmocked/friends/send-friend-request.test.ts`         | `tests/mocked/friends/send-friend-request-M.test.ts`      | Friend DB, User DB    |
| **POST /api/friends/accept**        | `tests/unmocked/friends/accept-friend-request.test.ts`       | `tests/mocked/friends/accept-friend-request-M.test.ts`    | Friend DB, User DB    |
| **POST /api/friends/reject**        | `tests/unmocked/friends/reject-friend-request.test.ts`       | `tests/mocked/friends/reject-friend-request-M.test.ts`    | Friend DB, User DB    |
| **GET /api/friends/list**           | `tests/unmocked/friends/get-friends.test.ts`                 | `tests/mocked/friends/get-friends-M.test.ts`              | Friend DB, User DB    |
| **GET /api/friends/pending**        | `tests/unmocked/friends/get-pending-requests.test.ts`        | `tests/mocked/friends/get-pending-requests-M.test.ts`     | Friend DB, User DB    |
| **DELETE /api/friends/:friendId**   | `tests/unmocked/friends/remove-friend.test.ts`               | `tests/mocked/friends/remove-friend-M.test.ts`            | Friend DB, User DB    |
| **POST /api/auth/signin**           | `tests/unmocked/auth/signin.test.ts`                         | `tests/mocked/auth/signin-M.test.ts`                      | Google auth, User DB  |
| **POST /api/auth/signup**           | `tests/unmocked/auth/signup.test.ts`                         | `tests/mocked/auth/signup-M.test.ts`                      | Google auth, User DB  |
| **DELETE /api/user/profile**        | `tests/unmocked/user/delete-profile.test.ts`                 | `tests/mocked/user/delete-profile-M.test.ts`              | User DB               |
| **GET /api/user/profile**           | `tests/unmocked/user/get-profile.test.ts`                    | `tests/mocked/user/get-profile-M.test.ts`                 | User DB               |
| **GET /api/user/:id**               | `tests/unmocked/user/get-user-info-by-id.test.ts`            | `tests/mocked/user/get-user-info-by-id-M.test.ts`         | User DB               |
| **PUT /api/user/profile**           | `tests/unmocked/user/update-profile.test.ts`                 | `tests/mocked/user/update-profile-M.test.ts`              | User DB               |
| **POST /api/media/upload**          |                                                              | `tests/mocked/media/media-controller-M.test.ts`           | User DB               |

Integration tests are located in `tests/integration-tests/` and test our NHL service, media service, and middleware like errorhandler.

#### 2.1.2. Commit Hash Where Tests Run

Commit SHA: `dcb18176d01bdcc9733d3ed655c656fea990db2e`

#### 2.1.3. Explanation on How to Run the Tests

1. **Clone the Repository**:

   - Open your terminal and run:
     ```
     git clone https://github.com/cpen-321-rink-rivals/M2-hockey-prediction-app.git
     ```

2. **Navigate to the backend**

   ```
    cd backend
   ```

3. Run the tests with coverage

   ```
     npm test -- --coverage
   ```

### 2.2. GitHub Actions Configuration Location

`~/.github/workflows/backend-tests.yml`

### 2.3. Jest Coverage Report Screenshots for Tests Without Mocking

![Jest Coverage Without Mocking](/documentation/images/jest-coverage-without-mocking.png)

### 2.4. Jest Coverage Report Screenshots for Tests With Mocking

![Jest Coverage With Mocking](/documentation/images/jest-coverage-with-mocking.png)

### 2.5. Jest Coverage Report Screenshots for Both Tests With and Without Mocking

#### IMPORTANT - due to our reduced scope we have not achieved 100 % coverage. We have left out the testing of our challenges feature. This is the reason we do not acheive full coverage in the following files:

- challenges.controller.ts
- challenges.model.ts
- socket.service.ts
- socket.events.ts
- gameStatusSync.job.ts

The sockets are only used in our challenges feature as well as the game status sync that runs every minute and update challenge statuses in the backend.
Line 9 in storage.ts is not covered as well, as it is only active on the first built of the server where no IMAGES_DIR has been created. It is not really part of running the app.
And some lines in index.ts and database.ts are not covered as well, as you cannot really test without the app initialized or if the database disconnects.

![Jest Coverage Combined](/documentation/images/jest-coverage-both.png)

---

## 3. Back-end Test Specification: Tests of Non-Functional Requirements

#### This part of the testing was skipped due to reduced team sized as discussed with the professor

---

## 4. Front-end Test Specification

### 4.1. Location in Git of Front-end Test Suite:

`frontend/app/src/androidTest/java/com/cpen321/usermanagement`

### 4.2. Tests

- **Use Case: Send Friend Request**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                                               | **Test Case Steps**                                                                |
    | ------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------- |
    | 1. The user opens the Friends screen.                                                            | Launch the Friends screen with a fake repository and ViewModel.                    |
    | 2. The app shows an input field labeled "Enter friend code" and a button labeled "Send Request." | Check that the input field and "Send Request" button are visible on screen.        |
    | 3a. The user enters a valid friend code.                                                         | Type `"VALIDCODE"` into the input field.<br>Click the "Send Request" button.       |
    | 3a1. The app confirms that the friend request was sent.                                          | Verify that the text `"Friend request sent!"` is displayed.                        |
    | 3b. The user enters an invalid friend code.                                                      | Clear the text field and type `"INVALID"`.<br>Click the "Send Request" button.     |
    | 3b1. The app shows an error message indicating the failure.                                      | Verify that the text `"Failed to send request: Invalid friend code"` is displayed. |
    | 3c. The user tries to send an empty friend code.                                                 | Clear the text field.<br>Click the "Send Request" button.                          |
    | 3c1. The app prevents the action and displays a warning.                                         | Verify that the text `"Friend code cannot be empty"` is displayed.                 |

- **Use Case: Accept Friend Request**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                                | **Test Case Steps**                                                                                                      |
    | --------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
    | 1. The user opens the Friends screen with a pending request.                      | Launch the Friends screen with a fake repository containing pending requests.                                            |
    | 2. The app displays the "Pending Requests" section and a request from "Taylor."   | Verify that `"Pending Requests"` and `"Taylor"` are displayed.                                                           |
    | 3. The user clicks "Accept" next to the pending request.                          | Click the "Accept" button beside `"Taylor"`.                                                                             |
    | 4. The app confirms that the friend was added and moves them to the friends list. | Verify that `"Taylor"` remains visible under the friends list.<br>Verify that `"Friend request accepted!"` is displayed. |

- **Use Case: Reject Friend Request**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                              | **Test Case Steps**                                                                                            |
    | ------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
    | 1. The user opens the Friends screen with pending requests.                     | Launch the Friends screen with a fake repository containing pending requests.                                  |
    | 2. The app displays a pending request from "Taylor."                            | Verify that `"Taylor"` is visible.                                                                             |
    | 3. The user clicks "Reject" next to the request.                                | Click the "Reject" button beside `"Taylor"`.                                                                   |
    | 4. The app confirms that the request was rejected and removes it from the list. | Verify that `"Friend request rejected"` is displayed.<br>Wait until `"Taylor"` is no longer visible on screen. |

- **Use Case: Remove Friend**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                     | **Test Case Steps**                                                                    |
    | ---------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
    | 1. The user opens the Friends screen with an existing friend list.     | Launch the Friends screen with a fake repository containing `"Alex"`.                  |
    | 2. The app displays `"Alex"` with a "Remove" button beside their name. | Verify that `"Alex"` is visible on screen.                                             |
    | 3. The user clicks "Remove" next to `"Alex"`.                          | Click the first "Remove" button found.                                                 |
    | 4. The app confirms the removal and updates the list.                  | Wait until `"Alex"` no longer appears.<br>Verify that `"Friend removed"` is displayed. |

- **Use Case: View Friends List**

  - **Expected Behaviors:**

    | **Scenario Steps**                                               | **Test Case Steps**                                                           |
    | ---------------------------------------------------------------- | ----------------------------------------------------------------------------- |
    | 1. The user opens the Friends screen with friends in their list. | Launch the Friends screen with a fake repository containing existing friends. |
    | 2. The app displays all current friends.                         | Verify that `"Alex"` is visible on screen.                                    |

- **Use Case: View Friends List (empty list)**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                            | **Test Case Steps**                                              |
    | ----------------------------------------------------------------------------- | ---------------------------------------------------------------- |
    | 1. The user opens the Friends screen with no friends and no pending requests. | Launch the Friends screen with a fake repository in empty state. |
    | 2. The app displays a message indicating the empty state.                     | Verify that `"No friends yet."` is displayed on screen.          |

- **Use Case: View Tickets List**

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
    | 1. The user opens the Bingo Tickets screen. | Launch the Tickets screen with a fake repository and ViewModel. |
    | 2. The app displays the screen title and a list of tickets. | Verify that the text `"Bingo Tickets"` is displayed.<br>Verify that `"My First Ticket"` appears in the list. |
    | 3. The displayed list corresponds to the user’s stored tickets. | Confirm that the tickets shown match those in the fake repository data. |

- **Use Case: Delete Ticket**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                   | **Test Case Steps**                                                              |
    | -------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
    | 1. The user opens the Bingo Tickets screen with at least one ticket. | Launch the Tickets screen with a fake repository containing `"My First Ticket"`. |
    | 2. The app displays the list of existing tickets.                    | Verify that `"My First Ticket"` is visible on screen.                            |
    | 3. The user clicks the "Delete" button beside a ticket.              | Click the "Delete" button associated with `"My First Ticket"`.                   |
    | 4. The ticket is removed from the list and no longer visible.        | Wait until `"My First Ticket"` is no longer displayed on screen.                 |

- **Use Case: View Tickets (empty)**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                | **Test Case Steps**                                                                   |
    | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
    | 1. The user opens the Bingo Tickets screen with no saved tickets. | Configure the fake repository with `emptyState = true`.<br>Launch the Tickets screen. |
    | 2. The app shows an empty state message.                          | Verify that `"No bingo tickets yet."` is displayed on screen.                         |

- **Use Case: Create new Ticket**

  - **Expected Behaviors:**

    | **Scenario Steps**                                              | **Test Case Steps**                                                                                                                                             |
    | --------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
    | 1. The user navigates to the Create Ticket screen.              | Use `navManager.navigateToCreateTicket()` to move to the create ticket screen.                                                                                  |
    | 2. The user enters a ticket name and selects events for a game. | Retrieve a game from the fake NHL data.<br>Call `viewModel.createTicket()` with: <br>• `name = "Test Ticket"` <br>• 9 events (`"Event 1"` through `"Event 9"`). |
    | 3. The user returns to the Tickets screen.                      | Relaunch the Tickets screen using the same ViewModel.                                                                                                           |
    | 4. The newly created ticket appears in the list.                | Verify that `"Test Ticket"` is visible on screen.                                                                                                               |

- **Use Case: Toggle Bingo square**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                | **Test Case Steps**                                                                                             |
    | ----------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
    | 1. The user opens a specific Bingo Ticket detail view.            | Retrieve a ticket from the fake repository. Set the content to `TicketDetailScreen` using that ticket.          |
    | 2. The app displays a grid of bingo squares.                      | Verify that multiple nodes with content description `"Bingo Square"` are displayed.                             |
    | 3. The user clicks a square to cross it off.                      | Perform click action on the first node with content description `"Bingo Square"`.                               |
    | 4. The ViewModel updates the state of that square to crossed off. | Retrieve the updated ticket from the ViewModel’s state. Assert that the first `crossedOff` entry equals `true`. |

---

## 5. Automated Code Review Results

#### This part of the testing was skipped due to reduced team sized as discussed with the professor

---
