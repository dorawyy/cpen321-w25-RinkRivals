# Testing and Code Review

## 1. Change History

| **Change Date** | **Modified Sections** | **Rationale**                         |
| --------------- | --------------------- | ------------------------------------- |
| 26. Nov.        | 2.1.1                 | added some missing Tests to the table |
| 28. Nov.        | 4.2                   | Fixed frontent test specifications and added test result image |

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

**Setup instructions**

1. Sign in to the app using both test gmail accounts found in frontend/app/src/androidTest/java/com/cpen321/usermanagement/E2ETests/E2Etests.kt 
2. Ensure both accounts have no pre-existing friends or bingo tickets. If there are, delete them.
3. Press the button in E2Etests.kt to run 'E2Etests'

- **Friends feature tests**

  - **Expected Behaviors:**

| **Scenario Steps**                                        | **Test Case Steps**                                                                |
| --------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| **Send friend request** | |
| 1. User 2 opens the app and signs in.                 | Call `signIn(EMAIL_2)`. Wait for UI to settle.                                     |
| 2. The app shows the main screen.                     | Wait until node with tag `"nav_friends"` exists.                                   |
| 3. User 2 navigates to the Friends screen.            | Click node with tag `"nav_friends"`. Wait until `"friendsSectionHeader"` exists.   |
| 4. The app displays User 2’s friend code.             | Read text from node `"friendCodeText"` using `SemanticsProperties.Text`.           |
| 5. User 2 logs out.                                   | Click `"nav_profile"`, then `"signOutButton"`. Wait until `"signinButton"` exists. |
| 6. User 1 opens the app and signs in.                 | Call `signIn(EMAIL_1)`.                                                            |
| 7. User 1 navigates to the Friends screen.            | Click `"nav_friends"`. Wait until `"noFriendsText"` exists.                        |
| 8. The app shows the empty friends state.             | Assert `"noFriendsText"` exists.                                                   |
| 9. User 1 enters User 2’s friend code.                | Perform text input into `"addFriendInput"` using the captured code.                |
| 10. User 1 sends the friend request.                  | Click `"sendFriendRequestButton"` and wait.                                        |
| 11. User 1 logs out.                                  | Click `"nav_profile"`, then `"signOutButton"`. Wait until `"signinButton"` exists. |
| **Accept friend request** | |
| 12. User 2 signs in again.                            | Call `signIn(EMAIL_2)`.                                                            |
| 13. User 2 navigates to the Friends screen.           | Click `"nav_friends"`. Wait until `"pendingRequestRow"` exists.                    |
| 14. The app displays User 1’s pending friend request. | Assert node `"pendingRequestRow"` exists.                                          |
| 15. User 2 accepts the friend request.                | Click `"acceptFriendRequestButton"`. Wait until `"friendRow"` exists.              |
| 16. User 2 logs out again.                            | Click `"nav_profile"` → `"signOutButton"`. Wait until `"signinButton"` exists.     |
| **View friends** | |
| 17. User 1 signs in again.                            | Call `signIn(EMAIL_1)`.                                                            |
| 18. User 1 navigates back to the Friends screen.      | Click `"nav_friends"`. Wait until `"friendRow"` exists.                            |
| 19. The app displays User 2 as a friend.              | Assert `"friendRow"` is present.                                                   |
| **Remove friend** | |
| 20. User 1 removes User 2 from their friends list.    | Click `"removeFriendButton"` (using `useUnmergedTree = true`).                     |
| 21. The app updates the list to empty state.          | Wait until `"noFriendsText"` exists.                                               |
| 22. User 1 logs out.                                  | Click `"nav_profile"` → `"signOutButton"`.                                         |


- **Bingo ticket feature tests**

  - **Expected Behaviors:**

    | **Scenario Steps**                                                | **Test Case Steps**                                                                                       |
    | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
    | **Create ticket** | |
    | 1. User navigates to the Tickets screen.                          | Click node with tag `nav_tickets`.                                                                            |
    | 2. App shows the empty tickets state.                             | Assert that a node with tag `emptyAddTicketButton` exists.                                                    |
    | 3. User presses “Add Ticket”.                                     | Click node with tag `addTicketButton`.                                                                        |
    | 4. App displays ticket creation screen.                           | Assert node with tag `ticketNameTextField` exists.                                                            |
    | 5. User enters a ticket name.                                     | Input `"MyTestTicket"` into node with tag `ticketNameTextField`.                                              |
    | 6. User selects a game from dropdown.                             | Click node with tag `gameDropdown`, wait for the dropdown items, then click the first node whose tag starts with `gameDropdownItem_`.   |
    | 7. User fills all bingo squares by selecting events.              | For indices 0–8: click node `bingoSquare_i`, wait for event picker, click first item with tag prefix `eventPickerItem_`, then wait for creation screen to reappear. |
    | 8. User presses “Create Ticket”.                                  | Click node with tag `createTicketButton`.                                                                     |
    | **View tickets** | |
    | 9. App shows the ticket list with the newly created ticket.       | Wait for a node whose tag starts with `tickets_section_header_`, then expand the first such node.             |
    | 10. User expands the ticket section.                              | Click the first node returned by `onAllNodes(hasTestTagPrefix("tickets_section_header_"))`.                   |
    | **Delete ticket** | |
    | 11. User presses delete on the created ticket.                    | Identify the created ticket using its tag prefix `ticket_card_`, extract ID, then click node with tag `ticket_delete_button_<id>`.      |
    | 12. App removes the ticket and returns to empty state.            | Assert node with tag `emptyAddTicketButton` exists again.                                                     |

**test results:**
![Frontend test results](/documentation/images/FrontendTestResults)
---

## 5. Automated Code Review Results

#### This part of the testing was skipped due to reduced team sized as discussed with the professor

---
