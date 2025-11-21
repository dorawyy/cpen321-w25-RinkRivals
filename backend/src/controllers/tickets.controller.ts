import { Request, Response } from 'express';
import { Ticket } from '../models/tickets.model';
import { TicketType } from '../types/tickets.types';
import { computeTicketScore } from '../utils/score.util';

export const createBingoTicket = async (req: Request, res: Response) => {
  try {
    const { userId, name, game, events, score: incomingScore } = req.body as TicketType;

    // If client didn't provide crossedOff or score, use sensible defaults.
    // crossedOff defaults to all false; score can be computed from crossedOff.
    const crossedOff: boolean[] = Array.isArray((req.body as any).crossedOff)
      ? (req.body as any).crossedOff
      : Array(9).fill(false);

    const score = incomingScore ?? computeTicketScore(crossedOff);

    // create ticket with crossedOff and score in a single call
    const newTicket = await Ticket.create({
      userId,
      name,
      game,
      events,
      crossedOff,
      score,
    });

    res.status(201).json(newTicket);
  } catch (error) {
    console.error('Error creating bingo ticket:', error);
    res.status(500).json({ message: 'Server error' });
  }
};

export const getUserTickets = async (req: Request, res: Response) => {
  try {
    const { userId } = req.params;
    const tickets = await Ticket.find({ userId }).sort({ createdAt: -1 });
    res.json(tickets);
  } catch (error) {
    res.status(500).json({ message: 'Server error' });
  }
};

export const getTicketById = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const ticket = await Ticket.findById(id);
    if (!ticket) {
      return res.status(404).json({ message: 'Ticket not found' });
    }
    res.json(ticket);
  } catch (error) {
    console.error('Error fetching ticket:', error);
    res.status(500).json({ message: 'Server error' });
  }
};

export const deleteTicket = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const deleted = await Ticket.findByIdAndDelete(id);
    if (!deleted) {
      return res.status(404).json({ message: 'Ticket not found' });
    }
    res.json({ message: 'Ticket deleted successfully' });
  } catch (error) {
    res.status(500).json({ message: 'Server error' });
  }
};

export const updateCrossedOff = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    // Handle either { crossedOff: [...] } or [...] directly
    const crossedOff = Array.isArray(req.body) ? req.body : req.body.crossedOff;

    if (!Array.isArray(crossedOff)) {
      return res.status(400).json({ message: 'Invalid crossedOff format' });
    }

    const score = computeTicketScore(crossedOff);
    const updated = await Ticket.findByIdAndUpdate(
      id,
      { crossedOff, score },
      { new: true }
    );
    if (!updated) return res.status(404).json({ message: 'Ticket not found' });

    res.json(updated);
  } catch (err) {
    res.status(500).json({ error: (err as Error).message });
  }
};
