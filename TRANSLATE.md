
# Translation in Ardent  
Ardent is currently in the process of internationalizing. I hope to make this process transparent and encourage   
translators to participate.  
  
## I want to translate. How?  
First, create an account on crowdin. Our project page is   
[https://crowdin.com/project/ardent](https://crowdin.com/project/ardent). Then, you can request access. Notify   
Valyn or Adam on the Ardent support server of your intention to join the translation project as well!

**Next, read through the translation models below to understand how stuff works**

## Translation Models
Ardent translation is designed to follow a few simple rules, which follow.

- **Commands**
*Associated information*: **name** (used in Discord), **description**
- Each command can have **examples**. Since arguments, flags, and commands are translated, these must be as well. 
 - **Arguments** (formerly subcommands) act to delineate what type of action the user wants to do. For example, in **/profile help**, *help* is an argument.
 *Associated information*: **name** (used in Discord), **description**
 - **Flags** are a new phenomenon in Ardent. They act to put parameters on a command the user wants to execute. For example, in **/clear -n 4**, the **-n** flag limits the number of cleared messages to 4. Not all flags accept arguments! For example, **/clear -d** invokes the default clear parameters (10 messages, same channel), but does not accept an argument
 *Associated information*: **value** (what's used in discord. does not include the prefixed hyphen), **argument accepted** (*possibly null*), **description**

### For developers
Identifiers:

 - Commands: `$name`
 - Examples: `$name.ex[number]` e.g. clear.ex1
 - Arguments:
	 - name: `$name.arguments.$argumentName`
	 - readable name (includes instructions, possibly null): `$name.arguments.$argumentName.readable`
	 - description: `$name.arguments.$argumentName.description`
 - Flags:
	 - accepted argument (possibly null): `$name.flags.$flagName.accepted`
	 - description: `$name.flags.$flagName.description`
