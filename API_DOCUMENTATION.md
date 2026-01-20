# API Documentation

## Table of Contents

- [Base URL](#base-url)
- [Caching](#caching)
- [Tasks API](#tasks-api)
  - [Endpoints](#endpoints)
- [Task Lists API](#task-lists-api)
  - [Endpoints](#endpoints-1)

## Base URL

- **Production**: `https://daitodo.duckdns.org`

## Caching

The API supports HTTP caching with `Last-Modified` headers:

- **`Last-Modified`**: Returned in all GET/POST/PUT responses with the resource's modification time
- **`If-Modified-Since`**: Send with GET requests to receive `304 Not Modified` if unchanged
- **`If-Unmodified-Since`**: Send with PUT/DELETE requests to prevent conflicts (returns `412 Precondition Failed` if modified)

## Tasks API

The Tasks API allows to manage individual to-do items. It uses the HTTP protocol and the JSON format.

The API is based on the CRUD pattern. It has the following operations:

- Create a new task
- Get many tasks that you can filter by status, priority, and/or due date
- Get one task by its ID
- Update a task
- Delete a task

### Endpoints

#### Create a new task

- `POST /tasks`

Create a new task.

##### Request

The request body must contain a JSON object with the following properties:

- `description` (string) - The description of the task
- `dueDate` (string) - The due date in YYYY-MM-DD format
- `priority` (enum) - The priority level: `LOW`, `MEDIUM`, or `HIGH`
- `status` (enum) - The task status: `TODO`, `DOING`, or `DONE`

##### Response

The response body contains a JSON object with the following properties:

- `id` (integer) - The unique identifier of the task (auto-generated)
- `description` (string) - The description of the task
- `createdAt` (string) - The creation date (auto-generated)
- `dueDate` (string) - The due date
- `priority` (enum) - The priority level
- `status` (enum) - The task status

The response includes a `Last-Modified` header with the creation timestamp.

##### Status codes

- `201` (Created) - The task has been successfully created
- `400` (Bad Request) - The request body is invalid or missing required fields

#### Get many tasks

- `GET /tasks`

Get many tasks.

##### Request

The request can contain the following query parameters:

- `status` (optional) - Filter by task status: `TODO`, `DOING`, or `DONE`
- `priority` (optional) - Filter by priority: `LOW`, `MEDIUM`, or `HIGH`
- `dueDate` (optional) - Filter by due date in YYYY-MM-DD format

##### Response

The response body contains a JSON array of task objects with the following properties:

- `id` (integer) - The unique identifier of the task
- `description` (string) - The description of the task
- `createdAt` (string) - The creation date
- `dueDate` (string) - The due date
- `priority` (enum) - The priority level
- `status` (enum) - The task status

The response includes a `Last-Modified` header with the timestamp of the most recently modified task.

##### Status codes

- `200` (OK) - The tasks have been successfully retrieved
- `304` (Not Modified) - The tasks have not been modified since `If-Modified-Since` timestamp

#### Get one task

- `GET /tasks/{id}`

Get one task by its ID.

##### Request

The request path must contain the ID of the task.

##### Response

The response body contains a JSON object with the following properties:

- `id` (integer) - The unique identifier of the task
- `description` (string) - The description of the task
- `createdAt` (string) - The creation date
- `dueDate` (string) - The due date
- `priority` (enum) - The priority level
- `status` (enum) - The task status

The response includes a `Last-Modified` header with the modification timestamp.

##### Status codes

- `200` (OK) - The task has been successfully retrieved
- `304` (Not Modified) - The task has not been modified since `If-Modified-Since` timestamp
- `404` (Not Found) - The task does not exist

#### Update a task

- `PUT /tasks/{id}`

Update a task by its ID.

##### Request

The request path must contain the ID of the task.

The request body must contain a JSON object with the following properties:

- `description` (string) - The description of the task
- `dueDate` (string) - The due date in YYYY-MM-DD format
- `priority` (enum) - The priority level: `LOW`, `MEDIUM`, or `HIGH`
- `status` (enum) - The task status: `TODO`, `DOING`, or `DONE`

The request can include an `If-Unmodified-Since` header to prevent conflicts.

##### Response

The response body contains a JSON object with the following properties:

- `id` (integer) - The unique identifier of the task
- `description` (string) - The description of the task
- `createdAt` (string) - The creation date
- `dueDate` (string) - The due date
- `priority` (enum) - The priority level
- `status` (enum) - The task status

The response includes a `Last-Modified` header with the new modification timestamp.

##### Status codes

- `200` (OK) - The task has been successfully updated
- `400` (Bad Request) - The request body is invalid
- `404` (Not Found) - The task does not exist
- `412` (Precondition Failed) - The task has been modified since `If-Unmodified-Since` timestamp

#### Delete a task

- `DELETE /tasks/{id}`

Delete a task by its ID.

##### Request

The request path must contain the ID of the task.

The request can include an `If-Unmodified-Since` header to prevent conflicts.

##### Response

The response body is empty.

##### Status codes

- `204` (No Content) - The task has been successfully deleted
- `404` (Not Found) - The task does not exist
- `412` (Precondition Failed) - The task has been modified since `If-Unmodified-Since` timestamp

## Task Lists API

The Task Lists API allows to manage collections of tasks organized by name. It uses the HTTP protocol and the JSON format.

The API is based on the CRUD pattern. It has the following operations:

- Create a new task list
- Get many task lists that you can filter by name
- Get one task list by its ID
- Update a task list
- Delete a task list

### Endpoints

#### Create a new task list

- `POST /tasklists`

Create a new task list.

##### Request

The request body must contain a JSON object with the following properties:

- `name` (string) - The name of the task list
- `tasks` (array) - An array of task objects (can be empty)

##### Response

The response body contains a JSON object with the following properties:

- `id` (integer) - The unique identifier of the task list (auto-generated)
- `name` (string) - The name of the task list
- `tasks` (array) - An array of task objects

The response includes a `Last-Modified` header with the creation timestamp.

##### Status codes

- `201` (Created) - The task list has been successfully created
- `400` (Bad Request) - The request body is invalid or missing required fields
- `409` (Conflict) - A task list with the same name already exists

#### Get many task lists

- `GET /tasklists`

Get many task lists.

##### Request

The request can contain the following query parameter:

- `name` (optional) - Filter by task list name (partial match)

##### Response

The response body contains a JSON array of task list objects with the following properties:

- `id` (integer) - The unique identifier of the task list
- `name` (string) - The name of the task list
- `tasks` (array) - An array of task objects

The response includes a `Last-Modified` header with the timestamp of the most recently modified task list.

##### Status codes

- `200` (OK) - The task lists have been successfully retrieved
- `304` (Not Modified) - The task lists have not been modified since `If-Modified-Since` timestamp

#### Get one task list

- `GET /tasklists/{id}`

Get one task list by its ID.

##### Request

The request path must contain the ID of the task list.

##### Response

The response body contains a JSON object with the following properties:

- `id` (integer) - The unique identifier of the task list
- `name` (string) - The name of the task list
- `tasks` (array) - An array of task objects

The response includes a `Last-Modified` header with the modification timestamp.

##### Status codes

- `200` (OK) - The task list has been successfully retrieved
- `304` (Not Modified) - The task list has not been modified since `If-Modified-Since` timestamp
- `404` (Not Found) - The task list does not exist

#### Update a task list

- `PUT /tasklists/{id}`

Update a task list by its ID.

##### Request

The request path must contain the ID of the task list.

The request body must contain a JSON object with the following properties:

- `name` (string) - The name of the task list
- `tasks` (array) - An array of task objects

The request can include an `If-Unmodified-Since` header to prevent conflicts.

##### Response

The response body contains a JSON object with the following properties:

- `id` (integer) - The unique identifier of the task list
- `name` (string) - The name of the task list
- `tasks` (array) - An array of task objects

The response includes a `Last-Modified` header with the new modification timestamp.

##### Status codes

- `200` (OK) - The task list has been successfully updated
- `400` (Bad Request) - The request body is invalid
- `404` (Not Found) - The task list does not exist
- `412` (Precondition Failed) - The task list has been modified since `If-Unmodified-Since` timestamp

#### Delete a task list

- `DELETE /tasklists/{id}`

Delete a task list by its ID.

##### Request

The request path must contain the ID of the task list.

The request can include an `If-Unmodified-Since` header to prevent conflicts.

##### Response

The response body is empty.

##### Status codes

- `204` (No Content) - The task list has been successfully deleted
- `404` (Not Found) - The task list does not exist
- `412` (Precondition Failed) - The task list has been modified since `If-Unmodified-Since` timestamp

## Usage Examples

For complete usage examples with curl commands and outputs, see the [Usage Examples section in the README](./README.md#usage-examples).
